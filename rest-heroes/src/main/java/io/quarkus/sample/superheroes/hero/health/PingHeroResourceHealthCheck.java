package io.quarkus.sample.superheroes.hero.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import io.quarkus.sample.superheroes.hero.graft.HeroesGraftFacade;

/**
 * {@link HealthCheck} to ping the hero Graftcode facade.
 */
@Liveness
public class PingHeroResourceHealthCheck implements HealthCheck {
  private final HeroesGraftFacade heroesGraftFacade;

  public PingHeroResourceHealthCheck(HeroesGraftFacade heroesGraftFacade) {
    this.heroesGraftFacade = heroesGraftFacade;
  }

  @Override
  public HealthCheckResponse call() {
    var response = this.heroesGraftFacade.hello();

    return HealthCheckResponse.named("Ping Hero Graft Facade")
      .withData("Response", response)
      .up()
      .build();
  }
}
