package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.config.SmallRyeConfig;

/**
 * This class is only needed until https://github.com/quarkiverse/quarkus-langchain4j/issues/130 is implemented.
 */
@QuarkusTest
class NarrationProcessorTests {
  private static final String REAL_NARRATION = "Real Narration";
  private static final String FALLBACK_NARRATION = "Fallback Narration";
  private static final Fight FIGHT = new Fight(
    "Han Solo",
    1000,
    "Big gun, doesn't believe in the force",
    "Storm Trooper",
    500,
    "Small gun",
    "Heroes",
    "Villains",
    new FightLocation(
      "Gotham City",
      "An American city rife with corruption and crime, the home of its iconic protector Batman."
    )
  );

  @InjectMock
  NarrationConfig narrationConfig;

  @InjectMock
  NarrationService narrationService;

  @Inject
  NarrationProcessor narrationProcessor;

  @BeforeEach
  public void beforeEach() {
    when(this.narrationService.narrate(FIGHT))
      .thenReturn(REAL_NARRATION);

    when(this.narrationService.narrateFallback(FIGHT))
      .thenReturn(FALLBACK_NARRATION);

    when(this.narrationConfig.fallbackNarration())
      .thenReturn(FALLBACK_NARRATION);
  }

  @Test
  void dontMakeRequests() {
    when(this.narrationConfig.makeLiveCalls())
      .thenReturn(false);

    assertThat(this.narrationProcessor.narrate(FIGHT))
      .isEqualTo(FALLBACK_NARRATION);

    verify(this.narrationConfig).makeLiveCalls();
    verify(this.narrationService, never()).narrate(any(Fight.class));
    verify(this.narrationService).narrateFallback(FIGHT);
    verifyNoMoreInteractions(this.narrationConfig, this.narrationService);
  }

  @Test
  void makeRequests() {
    when(this.narrationConfig.makeLiveCalls())
      .thenReturn(true);

    assertThat(this.narrationProcessor.narrate(FIGHT))
      .isEqualTo(REAL_NARRATION);

    verify(this.narrationConfig).makeLiveCalls();
    verify(this.narrationService).narrate(FIGHT);
    verify(this.narrationService, never()).narrateFallback(any(Fight.class));
    verifyNoMoreInteractions(this.narrationConfig, this.narrationService);
  }

  static class NarrationConfigMockProducer {
    @Inject
    Config config;

    @Produces
    @ApplicationScoped
    @Mock
    NarrationConfig narrationConfig() {
      return this.config.unwrap(SmallRyeConfig.class)
        .getConfigMapping(NarrationConfig.class);
    }
  }
}
