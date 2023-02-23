package io.quarkus.sample.superheroes.fight.service;

import jakarta.inject.Inject;

import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.test.junit.mockito.InjectSpy;

public abstract class FightServiceTestsBase {
  static final String DEFAULT_HERO_NAME = "Super Baguette";
  static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
  static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
  static final int DEFAULT_HERO_LEVEL = 42;
  static final String HEROES_TEAM_NAME = "heroes";
  static final String DEFAULT_HELLO_HERO_RESPONSE = "Hello heroes!";

  static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
  static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
  static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
  static final int DEFAULT_VILLAIN_LEVEL = 42;
  static final String VILLAINS_TEAM_NAME = "villains";
  static final String DEFAULT_HELLO_VILLAIN_RESPONSE = "Hello villains!";

  @Inject
  FightConfig fightConfig;

  @InjectSpy
  FightService fightService;

  static Hero createDefaultHero() {
		return new Hero(
			DEFAULT_HERO_NAME,
			DEFAULT_HERO_LEVEL,
			DEFAULT_HERO_PICTURE,
			DEFAULT_HERO_POWERS
		);
	}

  Villain createFallbackVillain() {
		return new Villain(
			this.fightConfig.villain().fallback().name(),
			this.fightConfig.villain().fallback().level(),
			this.fightConfig.villain().fallback().picture(),
			this.fightConfig.villain().fallback().powers()
		);
	}

  Hero createFallbackHero() {
		return new Hero(
			this.fightConfig.hero().fallback().name(),
			this.fightConfig.hero().fallback().level(),
			this.fightConfig.hero().fallback().picture(),
			this.fightConfig.hero().fallback().powers()
		);
	}

	static Villain createDefaultVillain() {
		return new Villain(
			DEFAULT_VILLAIN_NAME,
			DEFAULT_VILLAIN_LEVEL,
			DEFAULT_VILLAIN_PICTURE,
			DEFAULT_VILLAIN_POWERS
		);
	}
}
