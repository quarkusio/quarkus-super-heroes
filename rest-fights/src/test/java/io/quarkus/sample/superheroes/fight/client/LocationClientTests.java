package io.quarkus.sample.superheroes.fight.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.sample.superheroes.fight.client.LocationClientTests.LocationTestProfile;
import io.quarkus.sample.superheroes.location.grpc.HelloRequest;
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsBlockingStub;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(LocationTestProfile.class)
public class LocationClientTests {
	private static final String DEFAULT_HELLO_RESPONSE = "Hello locations!";

	@ConfigProperty(name = "quarkus.grpc.clients.location.host")
	String locationClientHost;

	@ConfigProperty(name = "quarkus.grpc.clients.location.port")
	String locationClientPort;

	@GrpcClient("location")
	LocationsBlockingStub locationBlockingClient;

	private String doHello() {
		return this.locationBlockingClient.hello(HelloRequest.newBuilder().build())
			.getMessage();
	}

	@Test
	public void doHelloBlocking() {
		System.out.println("location host = " + this.locationClientHost);
		System.out.println("location port = " + this.locationClientPort);

		assertThat(doHello())
			.isNotNull()
			.isEqualTo(DEFAULT_HELLO_RESPONSE);
	}

	public static class LocationTestProfile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
				"quarkus.grpc.clients.location.host", "localhost",
		    "quarkus.grpc.clients.location.port", "9999"
	    );
		}
	}
}
