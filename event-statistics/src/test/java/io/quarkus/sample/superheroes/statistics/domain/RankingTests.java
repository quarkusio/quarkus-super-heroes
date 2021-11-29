package io.quarkus.sample.superheroes.statistics.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RankingTests {
	private Ranking ranking = new Ranking(3);

	@Test
	public void onNewScoreScenario() {
		assertThat(this.ranking.onNewScore(new Score("Score 1", 1)))
			.isNotNull()
			.hasSize(1)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(tuple("Score 1", 1));

		assertThat(this.ranking.onNewScore(new Score("Score 2", 2)))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 2", 2),
				tuple("Score 1", 1)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 1", 3)))
			.isNotNull()
			.hasSize(2)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 1", 3),
				tuple("Score 2", 2)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 3", 5)))
			.isNotNull()
			.hasSize(3)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 3", 5),
				tuple("Score 1", 3),
				tuple("Score 2", 2)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 4", 4)))
			.isNotNull()
			.hasSize(3)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 3", 5),
				tuple("Score 4", 4),
				tuple("Score 1", 3)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 4", 10)))
			.isNotNull()
			.hasSize(3)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 4", 10),
				tuple("Score 3", 5),
				tuple("Score 1", 3)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 1", 4)))
			.isNotNull()
			.hasSize(3)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 4", 10),
				tuple("Score 3", 5),
				tuple("Score 1", 4)
			);

		assertThat(this.ranking.onNewScore(new Score("Score 3", 3)))
			.isNotNull()
			.hasSize(3)
			.extracting(Score::getName, Score::getScore)
			.containsExactly(
				tuple("Score 4", 10),
				tuple("Score 1", 4),
				tuple("Score 3", 3)
			);
	}
}
