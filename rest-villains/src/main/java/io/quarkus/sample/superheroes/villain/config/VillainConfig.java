package io.quarkus.sample.superheroes.villain.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "villain")
public interface VillainConfig {
	Level level();

	interface Level {
		@WithDefault("1.0")
		double multiplier();
	}
}
