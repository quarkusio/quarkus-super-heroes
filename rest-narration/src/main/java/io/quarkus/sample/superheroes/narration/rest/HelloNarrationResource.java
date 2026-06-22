package io.quarkus.sample.superheroes.narration.rest;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.api.resources.HelloResource;

import io.smallrye.common.annotation.NonBlocking;

public class HelloNarrationResource implements HelloResource {
  @Override
  @NonBlocking
  public String hello() {
    Log.debug("Hello Narration Resource");
    return "Hello Narration Resource";
  }
}
