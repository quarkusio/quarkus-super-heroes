package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.service.NarrationServiceDisabledTests.DisabledTestProfile;

@QuarkusTest
@TestProfile(DisabledTestProfile.class)
class NarrationServiceDisabledTests {
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

  @Inject
  NarrationService narrationService;

  @Test
  void dontMakeRequests() {
    assertThat(this.narrationService.narrate(FIGHT))
      .isEqualTo(NarrationService.FALLBACK_NARRATION);
  }

  public static class DisabledTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
         "quarkus.langchain4j.openai.enable-integration", "false",
         "quarkus.langchain4j.azure-openai.enable-integration", "false"
      );
    }
  }
}
