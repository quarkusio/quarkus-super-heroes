package io.quarkus.sample.superheroes.fight.client;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;

import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

/**
 * Pact consumer contract tests for the {@link io.quarkus.sample.superheroes.fight.client.VillainClient}.
 */
@QuarkusTest
@Tag("NotSafeForContinuousTesting") // See https://github.com/quarkiverse/quarkus-pact/issues/58
@TestProfile(PactConsumerContractTestProfile.class)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
  providerName = "rest-villains",
  // Make an assumption and hard-code the Pact MockServer to be running on port 8081
  // I don't like it but couldn't figure out any other way
  port = "8081")
public class VillainConsumerContractTests extends VillainClientTestRunner {
  @Pact(consumer = "rest-fights")
  public V4Pact helloPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A hello request")
        .path(VILLAIN_HELLO_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher(".+", DEFAULT_HELLO_RESPONSE))
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights")
  public V4Pact randomVillainNotFoundPact(PactDslWithProvider builder) {
    return builder
      .given("No random villain found")
      .uponReceiving("A request for a random villain")
        .path(VILLAIN_RANDOM_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      .willRespondWith()
        .status(Status.NOT_FOUND.getStatusCode())
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights")
  public V4Pact randomVillainFoundPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A request for a random villain")
        .path(VILLAIN_RANDOM_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      .willRespondWith()
        .status(Status.OK.getStatusCode())
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
        .body(newJsonBody(body ->
            body
              .stringType("name", DEFAULT_VILLAIN_NAME)
              .integerType("level", DEFAULT_VILLAIN_LEVEL)
              .stringType("picture", DEFAULT_VILLAIN_PICTURE)
          ).build()
        )
      .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(pactMethod = "helloPact")
  void helloVillains() {
    runHelloVillains();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainNotFoundPact")
  void randomVillainNotFound() {
    runRandomVillainNotFound();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainFoundPact")
  void randomVillainFound() {
    runRandomVillainFound(false);
  }
}
