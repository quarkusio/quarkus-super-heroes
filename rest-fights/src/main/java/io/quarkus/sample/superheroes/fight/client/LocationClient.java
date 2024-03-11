package io.quarkus.sample.superheroes.fight.client;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.mapping.LocationMapper;
import io.quarkus.sample.superheroes.location.grpc.HelloReply;
import io.quarkus.sample.superheroes.location.grpc.HelloRequest;
import io.quarkus.sample.superheroes.location.grpc.Locations;
import io.quarkus.sample.superheroes.location.grpc.RandomLocationRequest;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class LocationClient {
	private final Locations locationClient;
  private final LocationMapper locationMapper;

	public LocationClient(@GrpcClient("locations") Locations locationClient, LocationMapper locationMapper) {
		this.locationClient = locationClient;
    this.locationMapper = locationMapper;
  }

	@CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("findRandomLocation")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @WithSpan(kind = SpanKind.CLIENT, value = "LocationClient.findRandomLocation")
	public Uni<FightLocation> findRandomLocation() {
		Log.debug("Making request to location service to find a random location");

		return this.locationClient.getRandomLocation(RandomLocationRequest.newBuilder().build())
      .map(this.locationMapper::fromGrpc)
			.invoke(location -> Log.debugf("Got random location from locations service: %s", location))
			.onFailure(this::isNotFoundFailure).recoverWithNull();
	}

	@WithSpan(kind = SpanKind.CLIENT, value = "LocationClient.helloLocations")
	public Uni<String> helloLocations() {
		Log.debug("Making request to location service for hello operation");

		return this.locationClient.hello(HelloRequest.newBuilder().build())
			.map(HelloReply::getMessage)
			.invoke(hello -> Log.debugf("Got response back from location service: %s", hello));
	}

	private boolean isNotFoundFailure(Throwable throwable) {
		return Optional.ofNullable(throwable)
			.filter(StatusRuntimeException.class::isInstance)
			.map(StatusRuntimeException.class::cast)
			.map(StatusRuntimeException::getStatus)
			.map(Status::getCode)
			.filter(code -> code == Code.NOT_FOUND)
			.isPresent();
	}
}
