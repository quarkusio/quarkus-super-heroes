package io.quarkus.sample.superheroes.fight.client;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;

import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import io.quarkus.sample.superheroes.fight.DisableIfContinuousTesting;

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
 * Pact consumer contract tests for the {@link io.quarkus.sample.superheroes.fight.client.HeroClient}.
 */
@QuarkusTest
@TestProfile(PactConsumerContractTestProfile.class)
@DisableIfContinuousTesting // See https://github.com/quarkiverse/quarkus-pact/issues/58
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(
  providerName = "rest-heroes",
  // Make an assumption and hard-code the Pact MockServer to be running on port 8081
  // I don't like it but couldn't figure out any other way
  port = "8081"
)
public class HeroConsumerContractTests extends HeroClientTestRunner {

  @Pact(consumer = "rest-fights")
  public V4Pact helloPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A hello request")
        .path(HERO_HELLO_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher(".+", DEFAULT_HELLO_RESPONSE))
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights")
  public V4Pact randomHeroNotFoundPact(PactDslWithProvider builder) {
    return builder
      .given("No random hero found")
      .uponReceiving("A request for a random hero")
        .path(HERO_RANDOM_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      .willRespondWith()
        .status(Status.NOT_FOUND.getStatusCode())
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights")
  public V4Pact randomHeroFoundPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A request for a random hero")
        .path(HERO_RANDOM_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
      .willRespondWith()
        .status(Status.OK.getStatusCode())
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
        .body(newJsonBody(body ->
            body
              .stringType("name", DEFAULT_HERO_NAME)
              .integerType("level", DEFAULT_HERO_LEVEL)
              .stringType("picture", DEFAULT_HERO_PICTURE)
          ).build()
        )
      .toPact(V4Pact.class);
  }

  @Test
  @PactTestFor(pactMethod = "helloPact")
  void helloHeroes() {
    runHelloHeroes();
  }

  @Test
  @PactTestFor(pactMethod = "randomHeroNotFoundPact")
  void randomHeroNotFound() {
    runRandomHeroNotFound();
  }

  @Test
  @PactTestFor(pactMethod = "randomHeroFoundPact")
  void randomHeroFound() {
    runRandomHeroFound(false);
  }
}
