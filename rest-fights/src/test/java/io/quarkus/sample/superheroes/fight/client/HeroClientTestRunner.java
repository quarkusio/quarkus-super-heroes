package io.quarkus.sample.superheroes.fight.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.inject.Inject;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * This class exists so that some of the tests and assertions with the Hero service
 * can be re-used between the Pact consumer contract tests and the actual tests
 * that use WireMock and perform more thorough verifications that don't belong
 * in a consumer contract test.
 */
abstract class HeroClientTestRunner {
  protected static final String HERO_API_BASE_URI = "/api/heroes";
  protected static final String HERO_RANDOM_URI = HERO_API_BASE_URI + "/random";
  protected static final String HERO_HELLO_URI = HERO_API_BASE_URI + "/hello";
  protected static final String DEFAULT_HERO_NAME = "Super Baguette";
  protected static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
  protected static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
  protected static final int DEFAULT_HERO_LEVEL = 42;
  protected static final String DEFAULT_HELLO_RESPONSE = "Hello heroes!";

  @Inject
  HeroClient heroClient;

  protected final void runHelloHeroes() {
    this.heroClient.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(DEFAULT_HELLO_RESPONSE);
  }

  protected final void runRandomHeroNotFound() {
    this.heroClient.findRandomHero()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(null);
  }

  protected final void runRandomHeroFound(boolean verifyPowers) {
    var hero = this.heroClient.findRandomHero()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(hero)
      .isNotNull()
      .extracting(
        Hero::getName,
        Hero::getLevel,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_HERO_NAME,
        DEFAULT_HERO_LEVEL,
        DEFAULT_HERO_PICTURE,
        verifyPowers ? DEFAULT_HERO_POWERS : null
      );
  }
}
