package io.quarkus.sample.superheroes.fight.health;

import javax.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.fight.rest.FightResource;

@Liveness
public class PingFightResourceHealthCheck implements HealthCheck {
	@Inject
	FightResource fightResource;

	@Override
	public HealthCheckResponse call() {
		String response = this.fightResource.hello();

		return HealthCheckResponse.named("Ping Fight REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
