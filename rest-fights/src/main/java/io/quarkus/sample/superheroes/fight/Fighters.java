package io.quarkus.sample.superheroes.fight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;

/**
 * Entity class representing Fighters
 */
@Schema(description = "A fight between one hero and one villain")
public record Fighters(@NotNull @Valid Hero hero, @NotNull @Valid Villain villain) {
  public Fighters(Fighters fighters) {
    this(fighters.hero(), fighters.villain());
  }
}
