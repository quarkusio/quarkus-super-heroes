package io.quarkus.sample.superheroes.statistics.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;

/**
 * Tests for the {@link io.quarkus.sample.superheroes.statistics.listener.TeamStats} class. Not a {@link io.quarkus.test.junit.QuarkusTest @QuarkusTest} because the test can simply call the methods with the appropriate input.
 */
class TeamStatsTests {
  private static final Fight HERO_WINNER = new Fight(
    "1",
    Instant.now(),
    "Chewbacca",
    2,
    "",
    "Darth Vader",
    1,
    "",
    "heroes",
    "villains"
  );

	private static final Fight VILLAIN_WINNER = new Fight(
    "2",
    Instant.now(),
    "Darth Vader",
    2,
    "",
    "Chewbacca",
    1,
    "",
    "villains",
    "heroes"
  );
	
	private TeamStats teamStats = new TeamStats();
	
	@Test
	public void teamStatsScenario() {
		assertThat(this.teamStats.add(HERO_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(1, 0, 1, (double) 1/1);

		assertThat(this.teamStats.add(VILLAIN_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(1, 1, 2, (double) 1/2);

		assertThat(this.teamStats.add(HERO_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(2, 1, 3, (double) 2/3);

		assertThat(this.teamStats.add(VILLAIN_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(2, 2, 4, (double) 2/4);

		assertThat(this.teamStats.add(HERO_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(3, 2, 5, (double) 3/5);

		assertThat(this.teamStats.add(VILLAIN_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(3, 3, 6, (double) 3/6);

		assertThat(this.teamStats.add(HERO_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(4, 3, 7, (double) 4/7);

		assertThat(this.teamStats.add(VILLAIN_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(4, 4, 8, (double) 4/8);

		assertThat(this.teamStats.add(HERO_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(5, 4, 9, (double) 5/9);

		assertThat(this.teamStats.add(VILLAIN_WINNER))
      .isNotNull()
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(5, 5, 10, (double) 5/10);

		assertThat(this.teamStats.getHeroesCount())
			.isEqualTo(5);

		assertThat(this.teamStats.getVillainsCount())
			.isEqualTo(5);
	}
}
