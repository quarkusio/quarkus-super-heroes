package io.quarkus.sample.superheroes.fight.client;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.client.locations.grpc.HelloReply;
import io.quarkus.sample.superheroes.fight.client.locations.grpc.HelloRequest;
import io.quarkus.sample.superheroes.fight.client.locations.grpc.Locations;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class LocationClient {
	private final Locations locationClient;

	public LocationClient(@GrpcClient("locations") Locations locationClient) {
		this.locationClient = locationClient;
	}

	@WithSpan(kind = SpanKind.CLIENT, value = "LocationClient.helloLocations")
	public Uni<String> helloLocations() {
		Log.debug("Making request to location service");

		return this.locationClient.hello(HelloRequest.newBuilder().build())
			.map(HelloReply::getMessage)
			.invoke(hello -> Log.debugf("Got response back from location service: %s", hello));
	}
}
