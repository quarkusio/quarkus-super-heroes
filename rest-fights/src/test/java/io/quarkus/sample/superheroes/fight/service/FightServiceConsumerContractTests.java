package io.quarkus.sample.superheroes.fight.service;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

/**
 * Additional tests for the service layer ({@link FightService} that also
 * act as Pact consumer contract tests for the {@link io.quarkus.sample.superheroes.fight.client.VillainClient}
 * and {@link io.quarkus.sample.superheroes.fight.client.HeroClient}.
 *
 * <p>
 *   Ideally these tests would be inside {@link FightServiceTests} directly but it can't
 *   because of https://github.com/pact-foundation/pact-jvm/issues/1674.
 *   <p>
 *   We can't take that version of Pact on the Quarkus 2.x branch for a number of reasons.
 *   When we get to Quarkus 3 then we will merge these tests back into {@link FightServiceTests}.
 *   </p>
 * </p>
 * <p>
 *   All the <strong>findRandomFighters</strong> variant methods have multiple permutations
 *   because of https://github.com/pact-foundation/pact-jvm/issues/1675. Once that is resolved
 *   then the number of <strong>findRandomFighters</strong> variant methods will get cut in half.
 *   <p>
 *   We can't take that version of Pact on the Quarkus 2.x branch for a number of reasons.
 *   When we get to Quarkus 3 then we will merge these tests back into {@link FightServiceTests}.
 *   </p>
 * </p>
 */
@QuarkusTest
@QuarkusTestResource(value = PactConsumerContractTestResource.class, restrictToAnnotatedClass = true)
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V4, mockServerImplementation = MockServerImplementation.KTorServer)
@TestMethodOrder(OrderAnnotation.class)
public class FightServiceConsumerContractTests extends FightServiceTestsBase {
  private static final String VILLAIN_API_BASE_URI = "/api/villains";
  private static final String VILLAIN_RANDOM_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";
  private static final String VILLAINS_MOCK_PORT = "8083";

  private static final String HERO_API_BASE_URI = "/api/heroes";
  private static final String HERO_RANDOM_URI = HERO_API_BASE_URI + "/random";
  private static final String HERO_HELLO_URI = HERO_API_BASE_URI + "/hello";
  private static final String HEROES_MOCK_PORT = "8084";

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
  @PactTestFor(pactMethod = "helloHeroesPact", port = HEROES_MOCK_PORT)
  @Order(0)
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
  @PactTestFor(pactMethod = "helloVillainsPact", port = VILLAINS_MOCK_PORT)
  @Order(0)
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

  private void findRandomFighters() {
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
  @PactTestFor(pactMethod = "randomHeroFoundPact", port = HEROES_MOCK_PORT)
  @Order(1)
  public void findRandomFightersHeroConsumerContract() {
    doReturn(Uni.createFrom().item(createDefaultVillain()))
      .when(this.villainClient)
      .findRandomVillain();

    findRandomFighters();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainFoundPact", port = VILLAINS_MOCK_PORT)
  @Order(1)
  public void findRandomFightersVillainConsumerContract() {
    doReturn(Uni.createFrom().item(createDefaultHero()))
      .when(this.heroClient)
      .findRandomHero();

    findRandomFighters();
  }

  private void findRandomFightersHeroNotFound() {
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
  @PactTestFor(pactMethod = "randomHeroNotFoundPact", port = HEROES_MOCK_PORT)
  @Order(2)
  public void findRandomFightersHeroNotFoundHeroConsumerContract() {
    doReturn(Uni.createFrom().item(createDefaultVillain()))
      .when(this.villainClient)
      .findRandomVillain();

    findRandomFightersHeroNotFound();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainFoundPact", port = VILLAINS_MOCK_PORT)
  @Order(2)
  public void findRandomFightersHeroNotFoundVillainConsumerContract() {
    doReturn(Uni.createFrom().nullItem())
      .when(this.heroClient)
      .findRandomHero();

    findRandomFightersHeroNotFound();
  }

  private void findRandomFightersVillainNotFound() {
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
  @PactTestFor(pactMethod = "randomHeroFoundPact", port = HEROES_MOCK_PORT)
  @Order(3)
  public void findRandomFightersVillainNotFoundHeroConsumerContract() {
    doReturn(Uni.createFrom().nullItem())
      .when(this.villainClient)
      .findRandomVillain();

    findRandomFightersVillainNotFound();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainNotFoundPact", port = VILLAINS_MOCK_PORT)
  @Order(3)
  public void findRandomFightersVillainNotFoundVillainConsumerContract() {
    doReturn(Uni.createFrom().item(createDefaultHero()))
      .when(this.heroClient)
      .findRandomHero();

    findRandomFightersVillainNotFound();
  }

  private void findRandomFightersNoneFound() {
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
  @PactTestFor(pactMethod = "randomHeroNotFoundPact", port = HEROES_MOCK_PORT)
  @Order(4)
  public void findRandomFightersNoneFoundHeroConsumerContract() {
    doReturn(Uni.createFrom().nullItem())
      .when(this.villainClient)
      .findRandomVillain();

    findRandomFightersNoneFound();
  }

  @Test
  @PactTestFor(pactMethod = "randomVillainNotFoundPact", port = VILLAINS_MOCK_PORT)
  @Order(4)
  public void findRandomFightersNoneFoundVillainConsumerContract() {
    doReturn(Uni.createFrom().nullItem())
      .when(this.heroClient)
      .findRandomHero();

    findRandomFightersNoneFound();
  }
}
