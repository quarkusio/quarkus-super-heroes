package io.quarkus.sample.superheroes.fight.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import javax.inject.Inject;

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * This class exists so that some of the tests and assertions with the Villain service
 * can be re-used between the Pact consumer contract tests and the actual tests
 * that use WireMock and perform more thorough verifications that don't belong
 * in a consumer contract test.
 */
abstract class VillainClientTestRunner {
  protected static final String VILLAIN_API_BASE_URI = "/api/villains";
  protected static final String VILLAIN_RANDOM_URI = VILLAIN_API_BASE_URI + "/random";
  protected static final String VILLAIN_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";

  protected static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
  protected static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
  protected static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
  protected static final int DEFAULT_VILLAIN_LEVEL = 42;
  protected static final String DEFAULT_HELLO_RESPONSE = "Hello villains!";

  @Inject
  VillainClient villainClient;

  protected final void runHelloVillains() {
    this.villainClient.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(DEFAULT_HELLO_RESPONSE);
  }

  protected final void runRandomVillainNotFound() {
    this.villainClient.findRandomVillain()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .assertItem(null);
  }

  protected final void runRandomVillainFound(boolean verifyPowers) {
    var villain = this.villainClient.findRandomVillain()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(villain)
      .isNotNull()
      .extracting(
        Villain::getName,
        Villain::getLevel,
        Villain::getPicture,
        Villain::getPowers
      )
      .containsExactly(
        DEFAULT_VILLAIN_NAME,
        DEFAULT_VILLAIN_LEVEL,
        DEFAULT_VILLAIN_PICTURE,
        verifyPowers ? DEFAULT_VILLAIN_POWERS : null
      );
  }
}
