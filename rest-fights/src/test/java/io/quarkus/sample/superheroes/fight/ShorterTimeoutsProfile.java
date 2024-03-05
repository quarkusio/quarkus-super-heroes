package io.quarkus.sample.superheroes.fight;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.service.FightService;
import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * The purpose of this {@link QuarkusTestProfile} is to override the default timeouts and circuit breakers for communication with the
 * external services.
 */
public class ShorterTimeoutsProfile implements QuarkusTestProfile {
  private static final String FIGHT_SERVICE_CLASS_NAME = FightService.class.getName();
  public static final int NARRATION_OVERRIDDEN_TIMEOUT = 1;
  public static final int HELLO_OVERRIDDEN_TIMEOUT = 1;
  public static final int FIND_RANDOM_OVERRIDDEN_TIMEOUT = 1;
  public static final int FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT = FIND_RANDOM_OVERRIDDEN_TIMEOUT * 2;
  public static final int CIRCUIT_BREAKER_OVERRIDDEN_DELAY = 1;

  @Override
  public Map<String, String> getConfigOverrides() {
    var vals = new HashMap<>(Map.of(
      "%s/narrateFight/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT),
      "%s/generateImageFromNarration/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT),
      "%s/helloHeroes/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "%s/helloVillains/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "%s/helloNarration/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "%s/helloLocations/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "%s/findRandomFighters/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT),
      "%s/findRandomLocation/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT),
      "%s/findRandomHero/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT),
      "%s/findRandomVillain/Timeout/value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT)
    ));

    vals.putAll(Map.of(
      "%s/findRandomVillain/CircuitBreaker/delay".formatted(VillainClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "%s/findRandomHero/CircuitBreaker/delay".formatted(HeroClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "%s/findRandomLocation/CircuitBreaker/delay".formatted(LocationClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "%s/narrateFight/CircuitBreaker/delay".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "%s/generateImageFromNarration/CircuitBreaker/delay".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY)
    ));

    return vals;
  }
}
