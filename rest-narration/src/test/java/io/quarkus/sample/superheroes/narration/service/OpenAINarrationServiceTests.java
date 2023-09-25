package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@Disabled("Not quite sure how to test this yet")
@QuarkusTest
@TestProfile(OpenAINarrationServiceTests.OpenAITestProfile.class)
class OpenAINarrationServiceTests {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final Fight FIGHT = new Fight(VILLAIN_NAME, VILLAIN_LEVEL, VILLAIN_POWERS, HERO_NAME, HERO_LEVEL, HERO_POWERS, VILLAIN_TEAM_NAME, HERO_TEAM_NAME);
  private static final ArgumentMatcher<Fight> FIGHT_MATCHER = FIGHT::equals;

  @InjectSpy
  OpenAINarrationService narrationService;

  @Inject
  NarrationConfig narrationConfig;

  @Test
  public void narrateReturnsFallback() {
    var narration = this.narrationService.narrate(FIGHT)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(45))
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(this.narrationConfig.fallbackNarration());

    verify(this.narrationService).narrate(argThat(FIGHT_MATCHER));
    verify(this.narrationService).fallbackNarrate(argThat(FIGHT_MATCHER));
  }

  public static class OpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
        "narration.open-ai.enabled", "true",
        "narration.open-ai.api-key", "MY_KEY",
        "narration.open-ai.organization-id", "MY_ORG"
      );
    }
  }
}
