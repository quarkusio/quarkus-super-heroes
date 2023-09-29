package io.quarkus.sample.superheroes.narration;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "The fight that is narrated")
public record Fight(
  String winnerName,
  int winnerLevel,
  String winnerPowers,
  String loserName,
  int loserLevel,
  String loserPowers,
  String winnerTeam,
  String loserTeam
) { }
