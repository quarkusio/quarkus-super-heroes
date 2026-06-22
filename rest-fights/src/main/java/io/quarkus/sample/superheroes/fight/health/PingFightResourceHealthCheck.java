package io.quarkus.sample.superheroes.fight.health;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.fight.rest.HelloFightResource;

/**
 * {@link HealthCheck} to ping the fight service
 */
@Liveness
public class PingFightResourceHealthCheck implements HealthCheck {
	@Inject
	HelloFightResource helloFightResource;

	@Override
	public HealthCheckResponse call() {
		String response = this.helloFightResource.hello().await().indefinitely();

		return HealthCheckResponse.named("Ping Fight REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
