package io.quarkus.sample.superheroes.fight.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * POJO representing a Villain response from the Villain service
 */
@Schema(description = "The villain fighting against the hero")
public record Villain(@NotEmpty String name, @NotNull int level, @NotEmpty String picture, String powers) {
  public Villain(Villain villain) {
    this(villain.name(), villain.level(), villain.picture(), villain.powers());
  }
}
