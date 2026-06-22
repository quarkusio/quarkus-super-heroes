package io.quarkus.sample.superheroes.fight.rest;

import io.quarkus.sample.superheroes.fight.api.resources.HelloResource;
import io.quarkus.sample.superheroes.fight.service.FightService;

import io.smallrye.mutiny.Uni;

/**
 * JAX-RS API endpoints with {@code /api/fights/hello} as the base URI for all endpoints
 */
public class HelloFightResource implements HelloResource {
  private final FightService service;

  public HelloFightResource(FightService service) {
    this.service = service;
  }

  @Override
  public Uni<String> hello() {
    return Uni.createFrom().item("Hello Fight Resource");
  }

  @Override
  public Uni<String> helloHeroes() {
    return this.service.helloHeroes();
  }

  @Override
  public Uni<String> helloVillains() {
    return this.service.helloVillains();
  }

  @Override
  public Uni<String> helloNarration() {
    return this.service.helloNarration();
  }

  @Override
  public Uni<String> helloLocations() {
    return this.service.helloLocations();
  }
}
