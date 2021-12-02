package io.quarkus.sample.superheroes.statistics.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data class for a fight. Uses <a href="https://projectlombok.org/">Lombok</a> for getters/setters/constructors/etc.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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
