package io.quarkus.sample.superheroes.fight.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * POJO representing a Hero response from the Hero service
 */
@Schema(description = "The hero fighting against the villain")
public record Hero(@NotEmpty String name, @NotNull int level, @NotEmpty String picture, String powers) {
  public Hero(Hero hero) {
    this(hero.name(), hero.level(), hero.picture(), hero.powers());
  }
}
