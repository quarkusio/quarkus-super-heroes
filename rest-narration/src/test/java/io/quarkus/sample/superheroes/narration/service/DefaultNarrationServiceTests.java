package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
class DefaultNarrationServiceTests {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final Fight FIGHT = new Fight(VILLAIN_NAME, VILLAIN_LEVEL, VILLAIN_POWERS, HERO_NAME, HERO_LEVEL, HERO_POWERS, VILLAIN_TEAM_NAME, HERO_TEAM_NAME);

  @Inject
  DefaultNarrationService narrationService;

  @Inject
  NarrationConfig narrationConfig;

  @Test
  public void narrateReturnsFallback() {
    var narration = this.narrationService.narrate(FIGHT)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(this.narrationConfig.fallbackNarration());
  }
}
