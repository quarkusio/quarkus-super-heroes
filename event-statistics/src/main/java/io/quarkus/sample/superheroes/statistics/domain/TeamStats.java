package io.quarkus.sample.superheroes.statistics.domain;

public class TeamStats {
	private int villains = 0;
	private int heroes = 0;

	public double add(Fight result) {
		if (result.getWinnerTeam().equalsIgnoreCase("heroes")) {
			this.heroes = this.heroes + 1;
		}
		else {
			this.villains = this.villains + 1;
		}

		return ((double) this.heroes / (this.heroes + this.villains));
	}

	public int getVillainsCount() {
		return this.villains;
	}

	public int getHeroesCount() {
		return this.heroes;
	}
}
