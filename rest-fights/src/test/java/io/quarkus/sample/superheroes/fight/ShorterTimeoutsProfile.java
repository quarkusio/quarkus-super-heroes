package io.quarkus.sample.superheroes.fight;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.service.FightService;

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
      "quarkus.fault-tolerance.\"%s/narrateFight\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/generateImageFromNarration\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(NARRATION_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/helloHeroes\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/helloVillains\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/helloNarration\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/helloLocations\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(HELLO_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/findRandomFighters\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_FIGHTERS_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/findRandomLocation\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/findRandomHero\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT),
      "quarkus.fault-tolerance.\"%s/findRandomVillain\".timeout.value".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(FIND_RANDOM_OVERRIDDEN_TIMEOUT)
    ));
    
    vals.putAll(Map.of(
      "quarkus.fault-tolerance.\"%s/narrateFight\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/generateImageFromNarration\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/helloHeroes\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/helloVillains\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/helloNarration\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/helloLocations\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomFighters\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomLocation\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomHero\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomVillain\".timeout.unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds"
    ));

    vals.putAll(Map.of(
      "quarkus.fault-tolerance.\"%s/findRandomVillain\".circuit-breaker.delay".formatted(VillainClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "quarkus.fault-tolerance.\"%s/findRandomHero\".circuit-breaker.delay".formatted(HeroClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "quarkus.fault-tolerance.\"%s/findRandomLocation\".circuit-breaker.delay".formatted(LocationClient.class.getName()), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "quarkus.fault-tolerance.\"%s/narrateFight\".circuit-breaker.delay".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "quarkus.fault-tolerance.\"%s/generateImageFromNarration\".circuit-breaker.delay".formatted(FIGHT_SERVICE_CLASS_NAME), String.valueOf(CIRCUIT_BREAKER_OVERRIDDEN_DELAY),
      "quarkus.fault-tolerance.\"%s/findRandomVillain\".circuit-breaker.delay-unit".formatted(VillainClient.class.getName()), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomHero\".circuit-breaker.delay-unit".formatted(HeroClient.class.getName()), "seconds",
      "quarkus.fault-tolerance.\"%s/findRandomLocation\".circuit-breaker.delay-unit".formatted(LocationClient.class.getName()), "seconds",
      "quarkus.fault-tolerance.\"%s/narrateFight\".circuit-breaker.delay-unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds",
      "quarkus.fault-tolerance.\"%s/generateImageFromNarration\".circuit-breaker.delay-unit".formatted(FIGHT_SERVICE_CLASS_NAME), "seconds"
    ));

    return vals;
  }
}
