package io.quarkus.sample.superheroes.fight.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Application-specific configuration
 */
@ConfigMapping(prefix = "fight")
public interface FightConfig {
	/**
	 * Processing configuration
	 */
	Process process();

	/**
	 * Hero configuration
	 */
	Hero hero();

	/**
	 * Villain configuration
	 */
	Villain villain();

	interface Process {
		/**
		 * The number of millis to add as a delay to the fight process. Can be used to introduce deliberate delays.
		 * <p>
		 *   Defaults to {@code 0}
		 * </p>
		 */
		@WithDefault("0")
		long delayMillis();
	}

	interface Hero {
		/**
		 * Hero team name
		 * <p>
		 *   Defaults to {@code heroes}
		 * </p>
		 */
		@WithDefault("heroes")
		String teamName();

		/**
		 * Hero fallback configuration
		 */
		HeroFallback fallback();

		/**
		 * An adjustment upper bound for the hero during a fight.
		 * <p>
		 *   Defaults to {@code 20}
		 * </p>
		 */
		@WithDefault("20")
		int adjustBound();

		interface HeroFallback {
			/**
			 * Hero fallback name
			 */
			String name();

			/**
			 * Hero fallback level
			 */
			int level();

			/**
			 * Hero fallback picture
			 */
			String picture();

			/**
			 * Hero fallback powers
			 */
			String powers();
		}
	}

	interface Villain {
		/**
		 * Villain team name
		 * <p>
		 *   Defaults to {@code villains}
		 * </p>
		 */
		@WithDefault("villains")
		String teamName();

		/**
		 * The base url for the villain service
		 */
		String clientBaseUrl();

		/**
		 * Villain fallback configuration
		 */
		VillainFallback fallback();

		/**
		 * An adjustment upper bound for the villain during a fight.
		 * <p>
		 *   Defaults to {@code 20}
		 * </p>
		 */
		@WithDefault("20")
		int adjustBound();

		interface VillainFallback {
			/**
			 * Villain fallback name
			 */
			String name();

			/**
			 * Villain fallback level
			 */
			int level();

			/**
			 * Villain fallback picture
			 */
			String picture();

			/**
			 * Villain fallback powers
			 */
			String powers();
		}
	}
}
