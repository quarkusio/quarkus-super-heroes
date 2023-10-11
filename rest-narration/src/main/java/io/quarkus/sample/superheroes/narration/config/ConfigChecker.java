package io.quarkus.sample.superheroes.narration.config;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.Startup;

/**
 * This class is needed to ensure that the {@link NarrationConfig} is valid upon application startup.
 * <p>If any of the validations fail then the application will fail to start up, which is what we want.</p>
**/
@Startup
@ApplicationScoped
public class ConfigChecker {
  private final NarrationConfig narrationConfig;

  public ConfigChecker(NarrationConfig narrationConfig) {
    this.narrationConfig = narrationConfig;
  }
}
