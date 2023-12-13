package io.quarkus.sample.superheroes.narration.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "narration")
public interface NarrationConfig {
  /**
   * Fallback narration
   */
  String fallbackNarration();

  /**
   * Whether or not to make live calls to the AI service. Defaults to {@code false}.
   * <p>
   *   Making live AI calls costs real money, so only enable this when you want to.
   * </p>
   * <p>
   *   The environment variable for setting this would be {@code NARRATION_MAKE_LIVE_CALLS}.
   * </p>
   * <p>
   *   This will only be needed until https://github.com/quarkiverse/quarkus-langchain4j/issues/130 is implemented.
   * </p>
   */
  @WithDefault("false")
  boolean makeLiveCalls();
}
