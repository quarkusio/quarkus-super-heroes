package io.quarkus.sample.superheroes.fight.service;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager} for handling Pact consumer
 * contract tests. Mostly here so that the Hero and Villain rest client URLs are
 * set to point to the Pact {@link au.com.dius.pact.consumer.MockServer MockServer} and
 * not the WireMock mocks.
 * <p>
 *   Also makes an assumption and hard-codes the Pact {@link au.com.dius.pact.consumer.MockServer MockServer}
 *   to be running on {@code localhost:8083} for the {@code rest-villains} service and on {@code localhost:8080} for the {@code rest-heroes} service.
 * </p>
 * <p>
 *   This application runs on port {@code 8082} and its tests run on a random port, so ports {@code 8083} and {@code 8080} should be available.
 * </p>
 */
public class PactConsumerContractTestResource implements QuarkusTestResourceLifecycleManager {
  @Override
  public Map<String, String> start() {
    // Make an assumption and hard-code the Pact MockServer to be running on ports 8083 & 8080
    // I don't like it but couldn't figure out any other way
    return Map.of(
      "quarkus.rest-client.hero-client.url", String.format("http://localhost:%s", FightServiceConsumerContractTests.HEROES_MOCK_PORT),
      "fight.villain.client-base-url", String.format("http://localhost:%s", FightServiceConsumerContractTests.VILLAINS_MOCK_PORT)
    );
  }

  @Override
  public void stop() {

  }
}
