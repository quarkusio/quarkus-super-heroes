package io.quarkus.sample.superheroes.hero.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.hero.rest.HelloHeroResource;

/**
 * {@link HealthCheck} to ping the Hero service
 */
@Liveness
public class PingHeroResourceHealthCheck implements HealthCheck {
	private final HelloHeroResource helloHeroResource;

  public PingHeroResourceHealthCheck(HelloHeroResource helloHeroResource) {
    this.helloHeroResource = helloHeroResource;
  }

  @Override
	public HealthCheckResponse call() {
		var response = this.helloHeroResource.hello().await().indefinitely();

		return HealthCheckResponse.named("Ping Hero REST Endpoint")
			.withData("Response", response)
			.up()
			.build();
	}
}
