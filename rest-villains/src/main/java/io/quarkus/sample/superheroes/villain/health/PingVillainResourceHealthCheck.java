package io.quarkus.sample.superheroes.villain.health;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.villain.rest.VillainResource;

/**
 * {@link HealthCheck} to ping the Villain service
 */
@Liveness
public class PingVillainResourceHealthCheck implements HealthCheck {
	@Inject
	VillainResource villainResource;

	@Override
	public HealthCheckResponse call() {
		var response = this.villainResource.hello();

		return HealthCheckResponse.named("Ping Villain REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
