package io.quarkus.sample.superheroes.hero.health;

import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.hero.rest.HeroResource;

/**
 * {@link HealthCheck} to ping the Hero service
 */
@Liveness
public class PingHeroResourceHealthCheck implements HealthCheck {
	@Inject
	HeroResource heroResource;

	@Override
	public HealthCheckResponse call() {
		var response = this.heroResource.hello();

		return HealthCheckResponse.named("Ping Hero REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
