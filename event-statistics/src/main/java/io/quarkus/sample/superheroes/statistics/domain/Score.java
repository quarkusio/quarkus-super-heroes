package io.quarkus.sample.superheroes.statistics.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

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
