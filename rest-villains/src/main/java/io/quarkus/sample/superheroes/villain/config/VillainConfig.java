package io.quarkus.sample.superheroes.villain.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration class for the <code>villain</code> prefix.
 */
@ConfigMapping(prefix = "villain")
public interface VillainConfig {
	/**
	 * The <code>villain.level</code> configuration item
	 */
	Level level();

	interface Level {
		/**
		 * The <code>villain.level.multiplier</code> configuration item. Defaults to <code>1.0</code>.
		 */
		@WithDefault("1.0")
		double multiplier();
	}
}
