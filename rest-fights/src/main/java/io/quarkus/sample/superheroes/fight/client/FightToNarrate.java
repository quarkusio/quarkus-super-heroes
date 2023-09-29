package io.quarkus.sample.superheroes.fight.client;

import jakarta.validation.constraints.NotEmpty;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Each fight has a winner and a loser")
public record FightToNarrate(
  @NotEmpty String winnerTeam,
  @NotEmpty String winnerName,
  @NotEmpty String winnerPowers,
  int winnerLevel,
  @NotEmpty String loserTeam,
  @NotEmpty String loserName,
  @NotEmpty String loserPowers,
  int loserLevel
) { }
