package io.quarkus.sample.superheroes.fight;

import java.util.Map;

import io.quarkus.sample.superheroes.fight.service.FightService;
import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The purpose of this {@link QuarkusTestProfile} is to override the default timeouts for communication with the
 * narration service. Its timeouts are rather high (30 seconds), so to simulate some tests that use fallback/retry
 * it makes the test execution time very high.
 */
public class NarrationShorterTimeoutsProfile implements QuarkusTestProfile {
  private static final String FIGHT_SERVICE_CLASS_NAME = FightService.class.getName();
  public static final int NARRATION_OVERRIDDEN_TIMEOUT = 3;

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
      "%s/narrateFight/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT),
      "%s/generateImageFromNarration/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT)
    );
  }
}
