package io.quarkus.sample.superheroes.statistics.domain;

import java.time.Instant;

import io.quarkus.runtime.annotations.RegisterForReflection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@RegisterForReflection
@Data
@AllArgsConstructor
@Builder
public class Fight {
	private Instant fightDate;
	private String winnerName;
	private int winnerLevel;
	private String winnerPicture;
	private String loserName;
	private int loserLevel;
	private String loserPicture;
	private String winnerTeam;
	private String loserTeam;
}
