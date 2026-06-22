package io.quarkus.sample.superheroes.villain.health;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.villain.rest.HelloVillainResource;

/**
 * {@link HealthCheck} to ping the Villain service
 */
@Liveness
public class PingVillainResourceHealthCheck implements HealthCheck {
	@Inject
	HelloVillainResource helloVillainResource;

	@Override
	public HealthCheckResponse call() {
		var response = this.helloVillainResource.hello();

		return HealthCheckResponse.named("Ping Villain REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
