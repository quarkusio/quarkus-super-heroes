package io.quarkus.sample.superheroes.statistics.listener;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.statistics.domain.Fight;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamStats;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class SuperStatsTests {
	private static final Instant NOW = Instant.now();
	private static final String HERO_NAME = "Chewbacca";
	private static final String VILLAIN_NAME = "Darth Vader";
	private SuperStats superStats = new SuperStats();

	@Test
	public void computeTeamStats() {
		// Create 10 fights, split between heroes and villains winning
		var fights = createSampleFights(true);

		// Call computeTeamStats and assert the items
		this.superStats.computeTeamStats(Multi.createFrom().items(fights))
			.subscribe().withSubscriber(AssertSubscriber.create(10))
			.assertSubscribed()
			.assertItems(
				(double) 1/1,
				(double) 1/2,
				(double) 2/3,
				(double) 2/4,
				(double) 3/5,
				(double) 3/6,
				(double) 4/7,
				(double) 4/8,
				(double) 5/9,
				(double) 5/10
			)
			.assertCompleted();

		// Get the computed stats and assert that 5 heroes and 5 villains won
		var stats = this.superStats.getTeamStats();
		assertThat(stats)
			.isNotNull()
			.extracting(TeamStats::getHeroesCount, TeamStats::getVillainsCount)
			.containsExactly(5, 5);
	}

	@Test
	public void computeTopWinners() {
		var fights = createSampleFights(false);

		List<Iterable<Score>> scores = this.superStats.computeTopWinners(Multi.createFrom().items(fights))
			.subscribe().withSubscriber(AssertSubscriber.create(10))
			.assertSubscribed()
			.assertCompleted()
			.getItems();

		assertThat(scores.get(0))
			.isNotNull()
			.hasSize(1)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(tuple(HERO_NAME, 1));

		assertThat(scores.get(1))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 1),
				tuple(VILLAIN_NAME, 1)
			);

		assertThat(scores.get(2))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 2),
				tuple(VILLAIN_NAME, 1)
			);

		assertThat(scores.get(3))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 2),
				tuple(VILLAIN_NAME, 2)
			);

		assertThat(scores.get(4))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 3),
				tuple(VILLAIN_NAME, 2)
			);

		assertThat(scores.get(5))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 3),
				tuple(VILLAIN_NAME, 3)
			);

		assertThat(scores.get(6))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 4),
				tuple(VILLAIN_NAME, 3)
			);

		assertThat(scores.get(7))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 4),
				tuple(VILLAIN_NAME, 4)
			);

		assertThat(scores.get(8))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 5),
				tuple(VILLAIN_NAME, 4)
			);

		assertThat(scores.get(9))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple(HERO_NAME, 5),
				tuple(VILLAIN_NAME, 5)
			);
	}

	private Stream<Fight> createSampleFights(boolean uniqueNames) {
		return IntStream.range(0, 10)
			.mapToObj(i -> {
				var heroName = HERO_NAME;
				var villainName = VILLAIN_NAME;

				if (uniqueNames) {
					heroName += "-" + i;
					villainName += "-" + i;
				}

				var fight = Fight.builder();

				if (i % 2 == 0) {
					fight = fight.winnerTeam("heroes")
						.loserTeam("villains")
						.winnerName(heroName)
						.loserName(villainName);
				}
				else {
					fight = fight.winnerTeam("villains")
						.loserTeam("heroes")
						.winnerName(villainName)
						.loserName(heroName);
				}

				return fight.build();
			});
	}
}
