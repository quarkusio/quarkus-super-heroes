package io.quarkus.sample.superheroes.fight.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.*;

import java.time.Duration;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.GrpcMockServerResource;
import io.quarkus.sample.superheroes.fight.InjectGrpcMock;
import io.quarkus.sample.superheroes.location.grpc.HelloReply;
import io.quarkus.sample.superheroes.location.grpc.HelloRequest;
import io.quarkus.sample.superheroes.location.grpc.Location;
import io.quarkus.sample.superheroes.location.grpc.LocationType;
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc;
import io.quarkus.sample.superheroes.location.grpc.RandomLocationRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.faulttolerance.api.CircuitBreakerMaintenance;
import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
@QuarkusTestResource(value = GrpcMockServerResource.class, restrictToAnnotatedClass = true)
public class LocationClientTests {
	private static final String DEFAULT_HELLO_RESPONSE = "Hello locations!";
	private static final String DEFAULT_LOCATION_NAME = "Gotham City";
	private static final String DEFAULT_LOCATION_DESCRIPTION = "Dark city where Batman lives.";
	private static final String DEFAULT_LOCATION_PICTURE = "gotham_city.png";
	private static final LocationType DEFAULT_LOCATION_TYPE = LocationType.PLANET;
	private static final Location DEFAULT_LOCATION = Location.newBuilder()
		.setName(DEFAULT_LOCATION_NAME)
		.setDescription(DEFAULT_LOCATION_DESCRIPTION)
		.setPicture(DEFAULT_LOCATION_PICTURE)
		.setType(DEFAULT_LOCATION_TYPE)
		.build();
  private static final FightLocation DEFAULT_FIGHT_LOCATION = new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION, DEFAULT_LOCATION_PICTURE);

	@Inject
	LocationClient locationClient;

	@InjectGrpcMock
	GrpcMock grpcMock;

	@Inject
	CircuitBreakerMaintenance circuitBreakerMaintenance;

	@BeforeEach
	public void beforeEach() {
		this.grpcMock.resetAll();
	}

	@AfterEach
  public void afterEach() {
    // Reset all circuit breaker counts after each test
    this.circuitBreakerMaintenance.resetAll();
  }

	@Test
	public void helloLocations() {
		this.grpcMock.register(
			unaryMethod(LocationsGrpc.getHelloMethod())
				.willReturn(HelloReply.newBuilder().setMessage(DEFAULT_HELLO_RESPONSE).build())
		);

		this.locationClient.helloLocations()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(DEFAULT_HELLO_RESPONSE);

		this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getHelloMethod())
				.withRequest(HelloRequest.newBuilder().build())
				.build(),
			times(1)
		);
	}

	@Test
	public void findsRandom() {
		this.grpcMock.register(
			unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
				.willReturn(DEFAULT_LOCATION)
		);

		IntStream.range(0, 5)
      .forEach(i -> {
        var location = this.locationClient.findRandomLocation()
          .subscribe().withSubscriber(UniAssertSubscriber.create())
          .assertSubscribed()
          .awaitItem(Duration.ofSeconds(10))
          .getItem();

        assertThat(location)
          .isNotNull()
	        .isEqualTo(DEFAULT_FIGHT_LOCATION);
      });

		this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getGetRandomLocationMethod())
				.withRequest(RandomLocationRequest.newBuilder().build())
				.build(),
			times(5)
		);
	}

	@Test
	public void findRandomRecoversFromNotFound() {
		this.grpcMock.register(
			unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
				.willReturn(Status.NOT_FOUND.withDescription("A location was not found"))
		);

		IntStream.range(0, 5)
			.forEach(i -> this.locationClient.findRandomLocation()
				.subscribe().withSubscriber(UniAssertSubscriber.create())
				.assertSubscribed()
				.awaitItem(Duration.ofSeconds(5))
				.assertItem(null)
			);

		this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getGetRandomLocationMethod())
				.withRequest(RandomLocationRequest.newBuilder().build())
				.build(),
			times(5)
		);
	}

	@Test
	public void findRandomDoesntRecoverFromError() {
		this.grpcMock.register(
			unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
				.willReturn(Status.UNAVAILABLE.withDescription("Service isn't there"))
		);

		// The way the circuit breaker works is that you have to fire at least requestVolumeThreshold
    // requests at the breaker before it starts to trip
    // This is so it can fill its window

    // Circuit breaker should trip after 2 calls to findRandomLocation
    // 1 Call = 1 actual call + 3 fallbacks = 4 total calls
		assertThat(this.circuitBreakerMaintenance.currentState("findRandomLocation"))
			.isEqualTo(CircuitBreakerState.CLOSED);

		// First 2 calls (and 3 subsequent retries) should just fail with WebApplicationException
    // While making actual calls to the service
		IntStream.rangeClosed(1, 2)
      .forEach(i ->
        this.locationClient.findRandomLocation()
          .subscribe().withSubscriber(UniAssertSubscriber.create())
          .assertSubscribed()
          .awaitFailure(Duration.ofSeconds(5))
          .assertFailedWith(StatusRuntimeException.class)
      );

		// Next call should trip the breaker
    // The breaker should not make an actual call
		var ex = this.locationClient.findRandomLocation()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitFailure(Duration.ofSeconds(5))
      .getFailure();

		assertThat(ex)
			.isNotNull()
			.isExactlyInstanceOf(CircuitBreakerOpenException.class)
			.hasMessageContainingAll(String.format("%s#findRandomLocation", LocationClient.class.getName()), "circuit breaker is open");

		// Verify that the breaker is open
    assertThat(this.circuitBreakerMaintenance.currentState("findRandomLocation"))
      .isEqualTo(CircuitBreakerState.OPEN);

		this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getGetRandomLocationMethod())
				.withRequest(RandomLocationRequest.newBuilder().build())
				.build(),
			times(8)
		);
	}
}
