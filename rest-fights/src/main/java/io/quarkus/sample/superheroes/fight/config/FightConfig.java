package io.quarkus.sample.superheroes.fight.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "fight")
public interface FightConfig {
	Process process();
	Hero hero();
	Villain villain();

	interface Process {
		@WithDefault("0")
		long delayMillis();
	}

	interface Hero {
		@WithDefault("heroes")
		String teamName();
		HeroFallback fallback();

		@WithDefault("20")
		int adjustBound();

		interface HeroFallback {
			String name();
			int level();
			String picture();
			String powers();
		}
	}

	interface Villain {
		@WithDefault("villains")
		String teamName();
		String clientBaseUrl();
		VillainFallback fallback();

		@WithDefault("20")
		int adjustBound();

		interface VillainFallback {
			String name();
			int level();
			String picture();
			String powers();
		}
	}
}
