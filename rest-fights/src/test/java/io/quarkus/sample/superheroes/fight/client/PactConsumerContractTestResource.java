package io.quarkus.sample.superheroes.fight.client;

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
  // Make an assumption and hard-code the Pact MockServer to be running on port 8081
  // I don't like it but couldn't figure out any other way
  private static final String URL = "http://localhost:8081";

  @Override
  public Map<String, String> start() {
//    return Map.of(
//      "quarkus.stork.hero-service.service-discovery.address-list", URL,
//      "quarkus.stork.villain-service.service-discovery.address-list", URL
//    );

    return Map.of(
      "quarkus.rest-client.hero-client.url", URL,
      "fight.villain.client-base-url", URL
    );
  }

  @Override
  public void stop() {

  }
}
