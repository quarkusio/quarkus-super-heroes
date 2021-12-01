package io.quarkus.sample.superheroes.statistics.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Data class for a score
 * <p>
 *   The {@link RegisterForReflection @RegisterForReflection} annotation instructs the native compilation to allow reflection access to the class. Without it, the serialization/deserialization would not work when running the native executable.
 * </p>
 */
@RegisterForReflection
public class Score {
	private String name;
	private int score;

	public Score() {
		this.score = 0;
	}

	public Score(String name, int score) {
		this.name = name;
		this.score = score;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getScore() {
		return this.score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "Score{" +
			"name='" + this.name + '\'' +
			", score=" + this.score +
			'}';
	}
}
