package io.quarkus.sample.superheroes.fight.service;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Map;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

import au.com.dius.pact.consumer.dsl.PactDslRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
@QuarkusTestResource(value = PactConsumerContractTestResource.class, restrictToAnnotatedClass = true)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V4)
@MockServerConfig(providerName = "rest-heroes", port = FightServiceConsumerContractTests.HEROES_MOCK_PORT, hostInterface = "localhost", implementation = MockServerImplementation.KTorServer)
@MockServerConfig(providerName = "rest-villains", port = FightServiceConsumerContractTests.VILLAINS_MOCK_PORT, hostInterface = "localhost", implementation = MockServerImplementation.KTorServer)
public class FightServiceConsumerContractTests extends FightServiceTestsBase {
  private static final String VILLAIN_API_BASE_URI = "/api/villains";
  private static final String VILLAIN_RANDOM_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";
  static final String VILLAINS_MOCK_PORT = "8083";

  private static final String HERO_API_BASE_URI = "/api/heroes";
  private static final String HERO_RANDOM_URI = HERO_API_BASE_URI + "/random";
  private static final String HERO_HELLO_URI = HERO_API_BASE_URI + "/hello";
  static final String HEROES_MOCK_PORT = "8080";

  @InjectSpy
  HeroClient heroClient;

  @InjectSpy
  VillainClient villainClient;

  @Pact(consumer = "rest-fights", provider = "rest-villains")
  public V4Pact helloVillainsPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A hello request")
        .path(VILLAIN_HELLO_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher(".+", DEFAULT_HELLO_VILLAIN_RESPONSE))
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights", provider = "rest-villains")
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

  @Pact(consumer = "rest-fights", provider = "rest-villains")
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

  @Pact(consumer = "rest-fights", provider = "rest-heroes")
  public V4Pact helloHeroesPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A hello request")
        .path(HERO_HELLO_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher(".+", DEFAULT_HELLO_HERO_RESPONSE))
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights", provider = "rest-heroes")
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

  @Pact(consumer = "rest-fights", provider = "rest-heroes")
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
  @PactTestFor(pactMethods = { "randomHeroNotFoundPact", "randomVillainNotFoundPact" })
	public void findRandomFightersNoneFound() {
		PanacheMock.mock(Fight.class);

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(new Fighters(createFallbackHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  @PactTestFor(pactMethods = { "randomHeroNotFoundPact", "randomVillainFoundPact" })
	public void findRandomFightersHeroNotFound() {
		PanacheMock.mock(Fight.class);

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
      .ignoringFields("hero.powers", "villain.powers")
			.isEqualTo(new Fighters(createFallbackHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  @PactTestFor(pactMethods = { "randomHeroFoundPact", "randomVillainNotFoundPact" })
	public void findRandomFightersVillainNotFound() {
		PanacheMock.mock(Fight.class);

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
      .ignoringFields("hero.powers", "villain.powers")
			.isEqualTo(new Fighters(createDefaultHero(), createFallbackVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  @PactTestFor(pactMethods = { "randomHeroFoundPact", "randomVillainFoundPact" })
	public void findRandomFighters() {
		PanacheMock.mock(Fight.class);

		var fighters = this.fightService.findRandomFighters()
			.subscribe().withSubscriber(UniAssertSubscriber.create())
			.assertSubscribed()
			.awaitItem(Duration.ofSeconds(5))
			.getItem();

		assertThat(fighters)
			.isNotNull()
			.usingRecursiveComparison()
      .ignoringFields("hero.powers", "villain.powers")
			.isEqualTo(new Fighters(createDefaultHero(), createDefaultVillain()));

		verify(this.heroClient).findRandomHero();
		verify(this.villainClient).findRandomVillain();
		verify(this.fightService).findRandomHero();
		verify(this.fightService).findRandomVillain();
		verify(this.fightService).addDelay(any(Uni.class));
		verify(this.fightService, never()).fallbackRandomHero();
		verify(this.fightService, never()).fallbackRandomVillain();
		PanacheMock.verifyNoInteractions(Fight.class);
	}

  @Test
  @PactTestFor(pactMethods = "helloHeroesPact")
  public void helloHeroesSuccess() {
    var message = this.fightService.helloHeroes()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo(DEFAULT_HELLO_HERO_RESPONSE);

    verify(this.heroClient).helloHeroes();
    verify(this.fightService).helloHeroes();
    verifyNoInteractions(this.villainClient);
  }

  @Test
  @PactTestFor(pactMethods = "helloVillainsPact")
  public void helloVillainsSuccess() {
    var message = this.fightService.helloVillains()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo(DEFAULT_HELLO_VILLAIN_RESPONSE);

    verify(this.villainClient).helloVillains();
    verify(this.fightService).helloVillains();
    verifyNoInteractions(this.heroClient);
  }
}
