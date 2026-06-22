package io.quarkus.sample.superheroes.villain.rest;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.villain.api.resources.HelloResource;

import io.smallrye.common.annotation.NonBlocking;

public class HelloVillainResource implements HelloResource {
  @Override
  @NonBlocking
	public String hello() {
    Log.debug("Hello Villain Resource");
		return "Hello Villain Resource";
	}
}
