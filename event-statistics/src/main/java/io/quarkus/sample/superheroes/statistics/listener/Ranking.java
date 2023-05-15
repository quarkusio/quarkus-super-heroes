package io.quarkus.sample.superheroes.statistics.listener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.sample.superheroes.statistics.domain.Score;

/**
 * Object used to compute a floating &quot;top&quot; winners. The number of winners to keep track of is defined at construction time.
 */
class Ranking {
	private static final Logger LOGGER = Logger.getLogger(Ranking.class);
	private static final Comparator<Score> SCORE_COMPARATOR = Comparator.comparingInt(s -> -1 * s.score());

	private final List<Score> topScores;
	private final int max;

	Ranking(int size) {
		this.max = size;
		this.topScores = new ArrayList<>(this.max);
	}

	/**
	 * Records a new {@link Score}
	 * @param score The {@link Score} received
	 * @return The current list of floating top winners and their scores
	 */
	synchronized Iterable<Score> onNewScore(Score score) {
		LOGGER.debugf("Adding score: %s", score);

		// Remove one if the name already exists
		this.topScores.removeIf(s -> Objects.equals(s.name(), score.name()));

		// Add the score
		this.topScores.add(score);

		// Sort the list and limit it to the max size
		var sortedScores = this.topScores.stream()
			.sorted(SCORE_COMPARATOR)
			.limit(this.max)
			.collect(Collectors.toUnmodifiableList());

		// Rebuild the list
		this.topScores.clear();
		this.topScores.addAll(sortedScores);

		LOGGER.debugf("Scores: %s", sortedScores);
		return sortedScores;
	}
}
