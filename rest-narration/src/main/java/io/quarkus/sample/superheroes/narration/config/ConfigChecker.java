package io.quarkus.sample.superheroes.narration.config;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class ConfigChecker {
  private final NarrationConfig narrationConfig;

  public ConfigChecker(NarrationConfig narrationConfig) {
    this.narrationConfig = narrationConfig;
  }
}
