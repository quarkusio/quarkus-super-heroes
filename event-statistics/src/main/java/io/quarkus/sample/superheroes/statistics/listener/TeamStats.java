package io.quarkus.sample.superheroes.statistics.listener;

import io.quarkus.sample.superheroes.fight.schema.Fight;

/**
 * Object keeping track of the number of battles won by heroes and villains
 */
class TeamStats {
	private int villains = 0;
	private int heroes = 0;

	/**
	 * Adds a {@link Fight}
	 * @param result The {@link Fight} received
	 * @return The running percentage of battles won by heroes
	 */
	double add(Fight result) {
		if (result.getWinnerTeam().equalsIgnoreCase("heroes")) {
			this.heroes = this.heroes + 1;
		}
		else {
			this.villains = this.villains + 1;
		}

		return ((double) this.heroes / (this.heroes + this.villains));
	}

	int getVillainsCount() {
		return this.villains;
	}

	int getHeroesCount() {
		return this.heroes;
	}
}
