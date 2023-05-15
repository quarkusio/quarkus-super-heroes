package io.quarkus.sample.superheroes.statistics.listener;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

/**
 * Tests for the {@link SuperStats} class. Not a {@link io.quarkus.test.junit.QuarkusTest @QuarkusTest} because the test can simply call the methods with the appropriate input.
 * <p>
 *   The listening capabilities will be tested in the integration tests.
 * </p>
 */
@QuarkusTest
class SuperStatsTests {
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String HERO_NAME = "Chewbacca";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final String VILLAIN_NAME = "Darth Vader";

  @InjectSpy
  SuperStats superStats;

  @Inject
  @Any
  InMemoryConnector inMemoryConnector;

  @Inject
  @Channel(SuperStats.TEAM_STATS_CHANNEL_NAME)
  Multi<TeamScore> teamStatsMulti;

  @Inject
  @Channel(SuperStats.TOP_WINNERS_CHANNEL_NAME)
  Multi<Iterable<Score>> topWinnersMulti;

  @Test
  public void processFight() {
    // Create the consumer subscriptions
    var teamScoresSubscription = this.teamStatsMulti
      .subscribe().withSubscriber(AssertSubscriber.create(10))
      .assertSubscribed();

    var topWinnersSubscription = this.topWinnersMulti
      .subscribe().withSubscriber(AssertSubscriber.create(10))
      .assertSubscribed();

    // Create 10 fights, split between heroes & villains winning
    createSampleFights()
      .forEach(fight -> this.inMemoryConnector.source(SuperStats.FIGHTS_CHANNEL_NAME).send(fight));

    // Get team scores
    var teamScores = teamScoresSubscription
      .awaitItems(10, Duration.ofSeconds(10))
      .getItems();

    // Verify team scores
    verifyTeamStats(teamScores);

    // Get top winners
    var topWinners = topWinnersSubscription
      .awaitItems(10, Duration.ofSeconds(10))
      .getItems();

    // Verify top winners
    verifyTopWinners(topWinners);
  }

  private void verifyTeamStats(List<TeamScore> scores) {
    assertThat(scores)
      .isNotNull()
      .hasSize(10)
      .extracting(
        TeamScore::heroWins,
        TeamScore::villainWins,
        TeamScore::getNumberOfFights,
        TeamScore::getHeroWinRatio
      )
      .containsExactly(
        tuple(1, 0, 1, (double) 1/1),
        tuple(1, 1, 2, (double) 1/2),
        tuple(2, 1, 3, (double) 2/3),
        tuple(2, 2, 4, (double) 2/4),
        tuple(3, 2, 5, (double) 3/5),
        tuple(3, 3, 6, (double) 3/6),
        tuple(4, 3, 7, (double) 4/7),
        tuple(4, 4, 8, (double) 4/8),
        tuple(5, 4, 9, (double) 5/9),
        tuple(5, 5, 10, (double) 5/10)
      );

    // Get the computed stats and assert that 5 heroes and 5 villains won
    assertThat(this.superStats.getTeamStats())
      .isNotNull()
      .extracting(TeamStats::getHeroesCount, TeamStats::getVillainsCount)
      .containsExactly(5, 5);
  }

  private void verifyTopWinners(List<Iterable<Score>> scores) {
    assertThat(scores.get(0))
      .isNotNull()
      .hasSize(1)
      .extracting(Score::name, Score::score)
      .containsExactly(tuple(HERO_NAME, 1));

    assertThat(scores.get(1))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 1),
        tuple(VILLAIN_NAME, 1)
      );

    assertThat(scores.get(2))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 2),
        tuple(VILLAIN_NAME, 1)
      );

    assertThat(scores.get(3))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 2),
        tuple(VILLAIN_NAME, 2)
      );

    assertThat(scores.get(4))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 3),
        tuple(VILLAIN_NAME, 2)
      );

    assertThat(scores.get(5))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 3),
        tuple(VILLAIN_NAME, 3)
      );

    assertThat(scores.get(6))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 4),
        tuple(VILLAIN_NAME, 3)
      );

    assertThat(scores.get(7))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 4),
        tuple(VILLAIN_NAME, 4)
      );

    assertThat(scores.get(8))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 5),
        tuple(VILLAIN_NAME, 4)
      );

    assertThat(scores.get(9))
      .isNotNull()
      .hasSize(2)
      .extracting(Score::name, Score::score)
      .containsExactly(
        tuple(HERO_NAME, 5),
        tuple(VILLAIN_NAME, 5)
      );
  }

  private Stream<Fight> createSampleFights() {
    return IntStream.range(0, 10)
      .mapToObj(i -> {
        var heroName = HERO_NAME;
        var villainName = VILLAIN_NAME;

        var fight = Fight.newBuilder()
          .setFightDate(Instant.now())
          .setWinnerLevel(2)
          .setLoserLevel(1);

        if (i % 2 == 0) {
          fight = fight.setWinnerTeam(HERO_TEAM_NAME)
            .setLoserTeam(VILLAIN_TEAM_NAME)
            .setWinnerName(heroName)
            .setLoserName(villainName);
        }
        else {
          fight = fight.setWinnerTeam(VILLAIN_TEAM_NAME)
            .setLoserTeam(HERO_TEAM_NAME)
            .setWinnerName(villainName)
            .setLoserName(heroName);
        }

        return fight.build();
      });
  }
}
