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

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.HeroClient;
import io.quarkus.sample.superheroes.fight.client.LocationClient;
import io.quarkus.sample.superheroes.fight.client.NarrationClient;
import io.quarkus.sample.superheroes.fight.client.VillainClient;
import io.quarkus.sample.superheroes.fight.service.FightServiceConsumerContractTests.PactConsumerContractTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
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
@TestProfile(PactConsumerContractTestProfile.class)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V4)
@MockServerConfig(
  providerName = "rest-heroes",
  port = FightServiceConsumerContractTests.HEROES_MOCK_PORT,
  hostInterface = "localhost",
  implementation = MockServerImplementation.KTorServer
)
@MockServerConfig(
  providerName = "rest-villains",
  port = FightServiceConsumerContractTests.VILLAINS_MOCK_PORT,
  hostInterface = "localhost",
  implementation = MockServerImplementation.KTorServer
)
@MockServerConfig(
  providerName = "rest-narration",
  port = FightServiceConsumerContractTests.NARRATION_MOCK_PORT,
  hostInterface = "localhost",
  implementation = MockServerImplementation.KTorServer
)
public class FightServiceConsumerContractTests extends FightServiceTestsBase {
  private static final String VILLAIN_API_BASE_URI = "/api/villains";
  private static final String VILLAIN_RANDOM_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";
  static final String VILLAINS_MOCK_PORT = "8083";

  private static final String HERO_API_BASE_URI = "/api/heroes";
  private static final String HERO_RANDOM_URI = HERO_API_BASE_URI + "/random";
  private static final String HERO_HELLO_URI = HERO_API_BASE_URI + "/hello";
  static final String HEROES_MOCK_PORT = "8080";

  private static final String NARRATION_API_BASE_URI = "/api/narration";
  private static final String NARRATION_NARRATE_URI = NARRATION_API_BASE_URI;
  private static final String NARRATION_HELLO_URI = NARRATION_NARRATE_URI + "/hello";
  static final String NARRATION_MOCK_PORT = "8085";


  @InjectSpy
  HeroClient heroClient;

  @InjectSpy
  VillainClient villainClient;

	@InjectSpy
	LocationClient locationClient;

  @InjectSpy
  @RestClient
  NarrationClient narrationClient;

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

  @Pact(consumer = "rest-fights", provider = "rest-narration")
  public V4Pact helloNarrationPact(PactDslWithProvider builder) {
    return builder
      .uponReceiving("A hello request")
        .path(NARRATION_HELLO_URI)
        .method(HttpMethod.GET)
        .headers(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher(".+", DEFAULT_HELLO_NARRATION_RESPONSE))
      .toPact(V4Pact.class);
  }

  @Pact(consumer = "rest-fights", provider = "rest-narration")
  public V4Pact narrateFightPact(PactDslWithProvider builder) {
    var fightersBody = newJsonBody(body ->
      body
        .stringType("winnerTeam", HEROES_TEAM_NAME)
        .stringType("winnerName", DEFAULT_HERO_NAME)
        .stringType("winnerPowers", DEFAULT_HERO_POWERS)
        .integerType("winnerLevel", DEFAULT_HERO_LEVEL)
        .stringType("loserTeam", VILLAINS_TEAM_NAME)
        .stringType("loserName", DEFAULT_VILLAIN_NAME)
        .stringType("loserPowers", DEFAULT_VILLAIN_POWERS)
        .integerType("loserLevel", DEFAULT_VILLAIN_LEVEL)
        .object(
          "location",
          location -> location
            .stringType("name", DEFAULT_LOCATION_NAME)
            .stringType("description", DEFAULT_LOCATION_DESCRIPTION)
          )
    ).build();

    return builder
      .uponReceiving("A request to narrate a fight")
        .path(NARRATION_NARRATE_URI)
        .method(HttpMethod.POST)
        .headers(
          HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN,
          HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON
        )
        .body(fightersBody)
      .willRespondWith()
        .headers(Map.of(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN))
        .status(Status.OK.getStatusCode())
        .body(PactDslRootValue.stringMatcher("[.\\s\\S]+", DEFAULT_NARRATION))
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
    verifyNoInteractions(this.narrationClient, this.locationClient);
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
    verifyNoInteractions(this.narrationClient, this.locationClient);
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
    verifyNoInteractions(this.narrationClient, this.locationClient);
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
    verifyNoInteractions(this.narrationClient, this.locationClient);
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
    verifyNoInteractions(this.villainClient, this.narrationClient, this.locationClient);
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
    verifyNoInteractions(this.heroClient, this.narrationClient, this.locationClient);
  }

  @Test
  @PactTestFor(pactMethods = "helloNarrationPact")
  public void helloNarrationSuccess() {
    PanacheMock.mock(Fight.class);
    var message = this.fightService.helloNarration()
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(message)
      .isNotNull()
      .isEqualTo(DEFAULT_HELLO_NARRATION_RESPONSE);

    verify(this.narrationClient).hello();
    verifyNoInteractions(this.heroClient, this.villainClient, this.locationClient);
    PanacheMock.verifyNoInteractions(Fight.class);
  }

  @Test
  @PactTestFor(pactMethods = "narrateFightPact")
  public void narrateFightSuccess() {
    PanacheMock.mock(Fight.class);
    var fightToNarrate = createFightToNarrateHeroWon();

    var narration = this.fightService.narrateFight(fightToNarrate)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(5))
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(DEFAULT_NARRATION);

    verify(this.narrationClient).narrate(eq(fightToNarrate));
		verify(this.fightService, never()).fallbackNarrateFight(eq(fightToNarrate));
    verifyNoInteractions(this.heroClient, this.villainClient, this.locationClient);
		PanacheMock.verifyNoInteractions(Fight.class);
  }

	public static class PactConsumerContractTestProfile implements QuarkusTestProfile {
		@Override
		public Map<String, String> getConfigOverrides() {
			return Map.of(
      "quarkus.rest-client.hero-client.url", String.format("http://localhost:%s", HEROES_MOCK_PORT),
      "fight.villain.client-base-url", String.format("http://localhost:%s", VILLAINS_MOCK_PORT),
      "quarkus.rest-client.narration-client.url", String.format("http://localhost:%s", NARRATION_MOCK_PORT)
			);
		}
	}
}
