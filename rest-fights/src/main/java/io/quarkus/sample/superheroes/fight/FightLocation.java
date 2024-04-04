package io.quarkus.sample.superheroes.fight;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Location of a fight")
public record FightLocation(String name, String description, String picture) {
  public FightLocation() {
    this(null, null, null);
  }
}
