package io.quarkus.sample.superheroes.narration;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Fight(
  String winnerName,
  int winnerLevel,
  String winnerPowers,
  String loserName,
  int loserLevel,
  String loserPowers,
  String winnerTeam,
  String loserTeam,
  FightLocation location
) {
  @RegisterForReflection
  public record FightLocation(String name, String description) { }
}
