package io.quarkus.sample.superheroes.statistics.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Ranking {
	private static final Logger LOGGER = Logger.getLogger(Ranking.class);
	private static final Comparator<Score> SCORE_COMPARATOR = Comparator.comparingInt(s -> -1 * s.getScore());

	private final List<Score> topScores;
	private final int max;

	public Ranking(int size) {
		this.max = size;
		this.topScores = new ArrayList<>(this.max);
	}

	public synchronized Iterable<Score> onNewScore(Score score) {
		LOGGER.debugf("Adding score: %s", score);

		// Remove one if the name already exists
		this.topScores.removeIf(s -> Objects.equals(s.getName(), score.getName()));

		// Add the score
		this.topScores.add(score);

		// Limit the list to max size
		var sortedScores = this.topScores.stream()
			.sorted(SCORE_COMPARATOR)
			.limit(this.max)
			.collect(Collectors.toUnmodifiableList());

		this.topScores.clear();
		this.topScores.addAll(sortedScores);

		LOGGER.debugf("Scores: %s", sortedScores);
		return sortedScores;
	}
}
