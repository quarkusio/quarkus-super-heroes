package io.quarkus.sample.superheroes.narration.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import io.smallrye.mutiny.Uni;

@LookupIfProperty(name = "narration.azure-open-ai.enabled", stringValue = "false", lookupIfMissing = true)
@ApplicationScoped
public class DefaultNarrationService implements NarrationService {
  private final NarrationConfig narrationConfig;

  public DefaultNarrationService(NarrationConfig narrationConfig) {
    this.narrationConfig = narrationConfig;
  }

  @Override
  public Uni<String> narrate(Fight fight) {
    return Uni.createFrom().item(this.narrationConfig.fallbackNarration())
      .invoke(() -> Log.info("Returning default narration"));
  }
}
