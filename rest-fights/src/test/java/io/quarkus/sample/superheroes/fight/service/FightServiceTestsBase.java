package io.quarkus.sample.superheroes.fight.service;

import java.time.Instant;
import java.util.Objects;

import jakarta.inject.Inject;

import org.bson.types.ObjectId;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate.FightToNarrateLocation;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.config.FightConfig;
import io.quarkus.test.junit.mockito.InjectSpy;

public abstract class FightServiceTestsBase {
  static final ObjectId DEFAULT_FIGHT_ID = new ObjectId();
  static final Instant DEFAULT_FIGHT_DATE = Instant.now();
  static final String DEFAULT_HERO_NAME = "Super Baguette";
  static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
  static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
  static final int DEFAULT_HERO_LEVEL = 42;
  static final String HEROES_TEAM_NAME = "heroes";
  static final String DEFAULT_HELLO_HERO_RESPONSE = "Hello heroes!";

  static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
  static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
  static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
  static final int DEFAULT_VILLAIN_LEVEL = 42;
  static final String VILLAINS_TEAM_NAME = "villains";
  static final String DEFAULT_HELLO_VILLAIN_RESPONSE = "Hello villains!";
  static final String DEFAULT_HELLO_NARRATION_RESPONSE = "Hello narration!";
	static final String DEFAULT_HELLO_LOCATION_RESPONSE = "Hello location!";
  static final String DEFAULT_NARRATION = """
                                          This is a default narration - NOT a fallback!
                                          
                                          High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                          With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                          
                                          In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                          The people knew that their protector had once again ensured their safety.
                                          """;
  static final String DEFAULT_LOCATION_NAME = "Gotham City";
  static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  static final String DEFAULT_LOCATION_PICTURE = "gotham_city.png";

  @Inject
  FightConfig fightConfig;

  @InjectSpy
  FightService fightService;

  static FightRequest createDefaultFightRequest() {
    return new FightRequest(createDefaultHero(), createDefaultVillain(), createDefaultFightLocation());
  }

  static Hero createDefaultHero() {
    return new Hero(
      DEFAULT_HERO_NAME,
      DEFAULT_HERO_LEVEL,
      DEFAULT_HERO_PICTURE,
      DEFAULT_HERO_POWERS
    );
  }

  static FightToNarrate createFightToNarrateHeroWon() {
    return new FightToNarrate(
      HEROES_TEAM_NAME,
      DEFAULT_HERO_NAME,
      DEFAULT_HERO_POWERS,
      DEFAULT_HERO_LEVEL,
      VILLAINS_TEAM_NAME,
      DEFAULT_VILLAIN_NAME,
      DEFAULT_VILLAIN_POWERS,
      DEFAULT_VILLAIN_LEVEL,
      new FightToNarrateLocation(createDefaultFightLocation())
    );
  }

  Villain createFallbackVillain() {
    return new Villain(
      this.fightConfig.villain().fallback().name(),
      this.fightConfig.villain().fallback().level(),
      this.fightConfig.villain().fallback().picture(),
      this.fightConfig.villain().fallback().powers()
    );
  }

  Hero createFallbackHero() {
    return new Hero(
      this.fightConfig.hero().fallback().name(),
      this.fightConfig.hero().fallback().level(),
      this.fightConfig.hero().fallback().picture(),
      this.fightConfig.hero().fallback().powers()
    );
  }

  static Villain createDefaultVillain() {
    return new Villain(
      DEFAULT_VILLAIN_NAME,
      DEFAULT_VILLAIN_LEVEL,
      DEFAULT_VILLAIN_PICTURE,
      DEFAULT_VILLAIN_POWERS
    );
  }

  static FightLocation createDefaultFightLocation() {
    return new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION, DEFAULT_LOCATION_PICTURE);
  }

  FightLocation createFallbackLocation() {
    return new FightLocation(
      this.fightConfig.location().fallback().name(),
      this.fightConfig.location().fallback().description(),
      this.fightConfig.location().fallback().picture()
    );
  }

  static ArgumentMatcher<Fight> fightMatcher(Fight fight) {
    return f -> (fight == f) || (
      (fight != null) &&
        (f != null) &&
        (Objects.equals(fight.fightDate, f.fightDate) || f.fightDate.isAfter(fight.fightDate)) &&
        Objects.equals(fight.id, f.id) &&
        Objects.equals(fight.loserLevel, f.loserLevel) &&
        Objects.equals(fight.loserName, f.loserName) &&
        Objects.equals(fight.loserPicture, f.loserPicture) &&
        Objects.equals(fight.loserPowers, f.loserPowers) &&
        Objects.equals(fight.loserTeam, f.loserTeam) &&
        Objects.equals(fight.winnerLevel, f.winnerLevel) &&
        Objects.equals(fight.winnerName, f.winnerName) &&
        Objects.equals(fight.winnerPicture, f.winnerPicture) &&
        Objects.equals(fight.winnerPowers, f.winnerPowers) &&
        Objects.equals(fight.winnerTeam, f.winnerTeam) &&
        Objects.equals(fight.location, f.location)
    );
  }
}
