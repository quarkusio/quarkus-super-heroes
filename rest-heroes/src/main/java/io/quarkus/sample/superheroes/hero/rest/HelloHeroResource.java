package io.quarkus.sample.superheroes.hero.rest;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.hero.api.resources.HelloResource;

import io.smallrye.mutiny.Uni;

public class HelloHeroResource implements HelloResource {
  @Override
	public Uni<String> hello() {
    Log.debug("Hello Hero Resource");
		return Uni.createFrom().item("Hello Hero Resource");
	}
}
