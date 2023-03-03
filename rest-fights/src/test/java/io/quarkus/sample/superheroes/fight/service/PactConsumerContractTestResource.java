package io.quarkus.sample.superheroes.fight.service;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Quarkus {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager for handling Pact consumer
 * contract tests. Mostly here so that the Hero and Villain rest client URLs are
 * set to point to the Pact {@link au.com.dius.pact.consumer.MockServer MockServer} and
 * not the WireMock mocks.
 * <p>
 *   Also makes an assumption and hard-codes the Pact {@link au.com.dius.pact.consumer.MockServer MockServer}
 *   to be running on {@code localhost:8081}.
 * </p>
 * <p>
 *   Quarkus itself is set to run its tests on a random port, so port {@code 8081} should be available.
 * </p>
 */
public class PactConsumerContractTestResource implements QuarkusTestResourceLifecycleManager {
  @Override
  public Map<String, String> start() {
    // Make an assumption and hard-code the Pact MockServer to be running on ports 8083 & 8084
    // I don't like it but couldn't figure out any other way
    return Map.of(
      "quarkus.rest-client.hero-client.url", "http://localhost:8084",
      "fight.villain.client-base-url", "http://localhost:8083"
    );
  }

  @Override
  public void stop() {

  }
}
