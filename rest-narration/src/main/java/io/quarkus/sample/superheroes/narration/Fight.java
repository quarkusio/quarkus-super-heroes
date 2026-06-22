package io.quarkus.sample.superheroes.narration;

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
  public record FightLocation(String name, String description) { }
}
