package io.quarkus.sample.superheroes.fight.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.ParameterizedTest.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.HeroesVillainsWiremockServerResource;
import io.quarkus.sample.superheroes.fight.InjectWireMock;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.serde.avro.ReflectAvroDatumProvider;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.Vertx;

/**
 * Integration tests for the application as a whole. Orders tests in an order to faciliate a scenario of interactions
 * <p>
 *   Uses wiremock to stub responses and verify interactions with the hero and villain services.
 * </p>
 * <p>
 *   Uses an external container image for Kafka
 * </p>
 * @see HeroesVillainsWiremockServerResource
 */
@QuarkusIntegrationTest
@QuarkusTestResource(HeroesVillainsWiremockServerResource.class)
@QuarkusTestResource(value = KafkaCompanionResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(OrderAnnotation.class)
public class FightResourceIT {
	private static final int DEFAULT_ORDER = 0;

  private static final String HERO_API_BASE_URI = "/api/heroes";
	private static final String HERO_API_URI = HERO_API_BASE_URI + "/random";
  private static final String HERO_API_HELLO_URI = HERO_API_BASE_URI + "/hello";
	private static final String HEROES_TEAM_NAME = "heroes";
	private static final String DEFAULT_HERO_NAME = "Super Baguette";
	private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
	private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
	private static final int DEFAULT_HERO_LEVEL = 62;

	private static final Hero DEFAULT_HERO = new Hero(
		DEFAULT_HERO_NAME,
		DEFAULT_HERO_LEVEL,
		DEFAULT_HERO_PICTURE,
		DEFAULT_HERO_POWERS
	);

  private static final String VILLAIN_API_BASE_URI = "/api/villains";
	private static final String VILLAIN_API_URI = VILLAIN_API_BASE_URI + "/random";
  private static final String VILLAIN_API_HELLO_URI = VILLAIN_API_BASE_URI + "/hello";
	private static final String VILLAINS_TEAM_NAME = "villains";
	private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
	private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_VILLAIN_LEVEL = 40;

	private static final String FALLBACK_HERO_NAME = "Fallback hero";
	private static final String FALLBACK_HERO_PICTURE = "https://dummyimage.com/280x380/1e8fff/ffffff&text=Fallback+Hero";
	private static final String FALLBACK_HERO_POWERS = "Fallback hero powers";
	private static final int FALLBACK_HERO_LEVEL = 1;

	private static final Hero FALLBACK_HERO = new Hero(
		FALLBACK_HERO_NAME,
		FALLBACK_HERO_LEVEL,
		FALLBACK_HERO_PICTURE,
		FALLBACK_HERO_POWERS
	);

	private static final Villain DEFAULT_VILLAIN = new Villain(
		DEFAULT_VILLAIN_NAME,
		DEFAULT_VILLAIN_LEVEL,
		DEFAULT_VILLAIN_PICTURE,
		DEFAULT_VILLAIN_POWERS
	);

	private static final String FALLBACK_VILLAIN_NAME = "Fallback villain";
	private static final String FALLBACK_VILLAIN_PICTURE = "https://dummyimage.com/280x380/b22222/ffffff&text=Fallback+Villain";
	private static final String FALLBACK_VILLAIN_POWERS = "Fallback villain powers";
	private static final int FALLBACK_VILLAIN_LEVEL = 45;

	private static final Villain FALLBACK_VILLAIN = new Villain(
		FALLBACK_VILLAIN_NAME,
		FALLBACK_VILLAIN_LEVEL,
		FALLBACK_VILLAIN_PICTURE,
		FALLBACK_VILLAIN_POWERS
	);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final int NB_FIGHTS = 3;

	@InjectWireMock
	WireMockServer wireMockServer;

	@InjectKafkaCompanion
  KafkaCompanion companion;

  private static Vertx vertx;

	@BeforeAll
	public static void beforeAll() {
		OBJECT_MAPPER.setSerializationInclusion(Include.NON_EMPTY);
    // Set Apicurio Avro
    vertx = Vertx.vertx();
    RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx));
  }

  @AfterAll
  static void afterAll() {
    Optional.ofNullable(vertx)
      .ifPresent(Vertx::close);
  }

	@BeforeEach
	public void beforeEach() {
		// Reset WireMock
		this.wireMockServer.resetAll();
    // Configure Avro Serde for Fight
    companion.setCommonClientConfig(Map.of(AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, ReflectAvroDatumProvider.class.getName()));
    Serde<io.quarkus.sample.superheroes.fight.schema.Fight> serde = Serdes.serdeFrom(new AvroKafkaSerializer<>(), new AvroKafkaDeserializer<>());
    serde.configure(companion.getCommonClientConfig(), false);
    companion.registerSerde(io.quarkus.sample.superheroes.fight.schema.Fight.class, serde);
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void helloEndpoint() {
		get("/api/fights/hello")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(TEXT)
				.body(is("Hello Fight Resource"));
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void pingOpenAPI() {
		get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersHeroFallback() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(serverError())
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(FALLBACK_HERO, DEFAULT_VILLAIN));

		this.wireMockServer.verify(4,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersVillainFallback() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(serverError())
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(DEFAULT_HERO, FALLBACK_VILLAIN));

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(4,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersHeroNotFound() {
		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(notFound())
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(FALLBACK_HERO, DEFAULT_VILLAIN));

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersVillainNotFound() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(notFound())
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(DEFAULT_HERO, FALLBACK_VILLAIN));

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersHeroAndVillainNotFound() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(notFound())
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(notFound())
		);

		var randomFighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(randomFighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(FALLBACK_HERO, FALLBACK_VILLAIN));

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersHeroAndVillainFallback() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(serverError())
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(serverError())
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(FALLBACK_HERO, FALLBACK_VILLAIN));

		this.wireMockServer.verify(4,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(4,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersAllOk() {
		resetCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
		);

		var fighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(fighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new Fighters(DEFAULT_HERO, DEFAULT_VILLAIN));

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(1,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);
	}

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloHeroesOk() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(HERO_API_HELLO_URI))
        .willReturn(okForContentType(TEXT_PLAIN, "Hello heroes!"))
    );

    get("/api/fights/hello/heroes")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello heroes!"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(HERO_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloHeroesFallback() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(HERO_API_HELLO_URI))
        .willReturn(
          okForContentType(TEXT_PLAIN, "Hello heroes!")
            .withFixedDelay(6 * 1000)
        )
    );

    get("/api/fights/hello/heroes")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Heroes microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(HERO_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloHeroesFail() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(HERO_API_HELLO_URI))
        .willReturn(serverError())
    );

    get("/api/fights/hello/heroes")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Heroes microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(HERO_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloVillainsOk() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(VILLAIN_API_HELLO_URI))
        .willReturn(okForContentType(TEXT_PLAIN, "Hello villains!"))
    );

    get("/api/fights/hello/villains")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello villains!"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(VILLAIN_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloVillainsFallback() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(VILLAIN_API_HELLO_URI))
        .willReturn(
          okForContentType(TEXT_PLAIN, "Hello villains!")
            .withFixedDelay(6 * 1000)
        )
    );

    get("/api/fights/hello/villains")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Villains microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(VILLAIN_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloVillainsFail() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(VILLAIN_API_HELLO_URI))
        .willReturn(serverError())
    );

    get("/api/fights/hello/villains")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Villains microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(VILLAIN_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

	@Test
	@Order(DEFAULT_ORDER)
	public void getAllFights() {
		getAndVerifyAllFights();
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void getFightNotFound() {
		get("/api/fights/{id}", new ObjectId().toString())
			.then().statusCode(NOT_FOUND.getStatusCode());
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void getFoundFight() {
    var expectedFight = new Fight();
    expectedFight.winnerName = "Chewbacca";
    expectedFight.winnerLevel = 5;
    expectedFight.winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg";
    expectedFight.winnerTeam = HEROES_TEAM_NAME;
    expectedFight.loserName = "Wanderer";
    expectedFight.loserLevel = 3;
    expectedFight.loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg";
    expectedFight.loserTeam = VILLAINS_TEAM_NAME;

		var fight = get("/api/fights/{id}", getAndVerifyAllFights().get(0).id.toString())
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fight.class);

    assertThat(fight)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFields("id", "fightDate")
      .isEqualTo(expectedFight);
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void performFightNullFighters() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/fights")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());
	}

	@ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
	@MethodSource("invalidFighters")
	public void performFightInvalidFighters(Fighters fighters) {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(fighters)
				.post("/api/fights")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());
	}

	@Test
	@Order(DEFAULT_ORDER + 2)
	public void performFightHeroWins() {
    var fights = companion.consume(io.quarkus.sample.superheroes.fight.schema.Fight.class)
      .withOffsetReset(OffsetResetStrategy.EARLIEST)
      .withGroupId("fights")
      .withAutoCommit()
      .fromTopics("fights", 1);

    var expectedFight = new Fight();
    expectedFight.winnerName = DEFAULT_HERO.getName();
    expectedFight.winnerLevel = DEFAULT_HERO.getLevel();
    expectedFight.winnerPicture = DEFAULT_HERO.getPicture();
    expectedFight.winnerTeam = HEROES_TEAM_NAME;
    expectedFight.loserName = DEFAULT_VILLAIN.getName();
    expectedFight.loserLevel = DEFAULT_VILLAIN.getLevel();
    expectedFight.loserPicture = DEFAULT_VILLAIN.getPicture();
    expectedFight.loserTeam = VILLAINS_TEAM_NAME;

		var fightResult = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(new Fighters(DEFAULT_HERO, DEFAULT_VILLAIN))
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Fight.class);

    assertThat(fightResult)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFields("id", "fightDate")
      .isEqualTo(expectedFight);

		get("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body("size()", is(NB_FIGHTS + 1));

    var fight = fights.awaitCompletion(Duration.ofSeconds(10))
      .getFirstRecord()
      .value();

		assertThat(fight)
			.isNotNull()
			.extracting(
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerName,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerLevel,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerPicture,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerTeam,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserName,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserLevel,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserPicture,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserTeam
			)
			.containsExactly(
				DEFAULT_HERO.getName(),
				DEFAULT_HERO.getLevel(),
				DEFAULT_HERO.getPicture(),
				HEROES_TEAM_NAME,
				DEFAULT_VILLAIN.getName(),
				DEFAULT_VILLAIN.getLevel(),
				DEFAULT_VILLAIN.getPicture(),
				VILLAINS_TEAM_NAME
			);
	}

	@Test
	@Order(DEFAULT_ORDER + 3)
	public void performFightVillainWins() {
    var fights = companion.consume(io.quarkus.sample.superheroes.fight.schema.Fight.class)
      .withOffsetReset(OffsetResetStrategy.EARLIEST)
      .withGroupId("fights")
      .withAutoCommit()
      .fromTopics("fights", 1);

    var fighters = new Fighters(
			new Hero(
				DEFAULT_HERO_NAME,
				DEFAULT_VILLAIN_LEVEL,
				DEFAULT_HERO_PICTURE,
				DEFAULT_HERO_POWERS
		),
			new Villain(
				DEFAULT_VILLAIN_NAME,
				DEFAULT_HERO_LEVEL,
				DEFAULT_VILLAIN_PICTURE,
				DEFAULT_VILLAIN_POWERS
			)
		);

    var expectedFight = new Fight();
    expectedFight.loserName = DEFAULT_HERO.getName();
    expectedFight.loserLevel = DEFAULT_VILLAIN.getLevel();
    expectedFight.loserPicture = DEFAULT_HERO.getPicture();
    expectedFight.loserTeam = HEROES_TEAM_NAME;
    expectedFight.winnerName = DEFAULT_VILLAIN.getName();
    expectedFight.winnerLevel = DEFAULT_HERO.getLevel();
    expectedFight.winnerPicture = DEFAULT_VILLAIN.getPicture();
    expectedFight.winnerTeam = VILLAINS_TEAM_NAME;

		var fightResult = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(fighters)
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Fight.class);

    assertThat(fightResult)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFields("id", "fightDate")
      .isEqualTo(expectedFight);

		get("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body("size()", is(NB_FIGHTS + 2));

    var fight = fights.awaitCompletion(Duration.ofSeconds(10))
      .getFirstRecord()
      .value();

		assertThat(fight)
			.isNotNull()
			.extracting(
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerName,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerLevel,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerPicture,
        io.quarkus.sample.superheroes.fight.schema.Fight::getWinnerTeam,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserName,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserLevel,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserPicture,
        io.quarkus.sample.superheroes.fight.schema.Fight::getLoserTeam
			)
			.containsExactly(
				DEFAULT_VILLAIN.getName(),
				DEFAULT_HERO.getLevel(),
				DEFAULT_VILLAIN.getPicture(),
				VILLAINS_TEAM_NAME,
				DEFAULT_HERO.getName(),
				DEFAULT_VILLAIN.getLevel(),
				DEFAULT_HERO.getPicture(),
				HEROES_TEAM_NAME
			);
	}

	private List<Fight> getAndVerifyAllFights() {
    var expectedFights = List.of(new Fight(), new Fight(), new Fight());
    expectedFights.get(0).winnerName = "Chewbacca";
    expectedFights.get(0).winnerLevel = 5;
    expectedFights.get(0).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg";
    expectedFights.get(0).winnerTeam = HEROES_TEAM_NAME;
    expectedFights.get(0).loserName = "Wanderer";
    expectedFights.get(0).loserLevel = 3;
    expectedFights.get(0).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg";
    expectedFights.get(0).loserTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(1).winnerName = "Galadriel";
    expectedFights.get(1).winnerLevel = 10;
    expectedFights.get(1).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/galadriel--1293733805363806029.jpg";
    expectedFights.get(1).winnerTeam = HEROES_TEAM_NAME;
    expectedFights.get(1).loserName = "Darth Vader";
    expectedFights.get(1).loserLevel = 8;
    expectedFights.get(1).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg";
    expectedFights.get(1).loserTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(2).winnerName = "Annihilus";
    expectedFights.get(2).winnerLevel = 23;
    expectedFights.get(2).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/annihilus--751928780106678215.jpg";
    expectedFights.get(2).winnerTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(2).loserName = "Shikamaru";
    expectedFights.get(2).loserLevel = 1;
    expectedFights.get(2).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/nara-shikamaru-1970614934047311432.jpg";
    expectedFights.get(2).loserTeam = HEROES_TEAM_NAME;

		var fights = get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.extract().body()
      .jsonPath().getList(".", Fight.class);

    assertThat(fights)
      .isNotNull()
      .hasSize(NB_FIGHTS)
      .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "fightDate")
      .containsExactlyElementsOf(expectedFights);

    return fights;
	}

	/**
	 * Reset the circuit breakers so they are in closed state
	 */
	private void resetCircuitBreakersToClosedState() {
		try {
			// Sleep the necessary delay duration for the breaker to moved into the half-open position
			TimeUnit.SECONDS.sleep(2);
		}
		catch (InterruptedException ex) {
			Log.error(ex.getMessage(), ex);
		}

		// Reset all the mocks on the WireMockServer
		this.wireMockServer.resetAll();

		// Stub successful requests
		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultVillainJson()))
		);

		// The circuit breaker requestVolumeThreshold == 8, so we need to make n+1 successful requests for it to clear
		IntStream.rangeClosed(0, 8)
			.forEach(i -> {
				var fighters = get("/api/fights/randomfighters").then()
					.statusCode(OK.getStatusCode())
					.contentType(JSON)
          .extract().as(Fighters.class);

        assertThat(fighters)
          .isNotNull()
          .usingRecursiveComparison()
          .isEqualTo(new Fighters(DEFAULT_HERO, DEFAULT_VILLAIN));
        }
			);

		// Verify successful requests
		this.wireMockServer.verify(9,
			getRequestedFor(urlEqualTo(HERO_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		this.wireMockServer.verify(9,
			getRequestedFor(urlEqualTo(VILLAIN_API_URI))
				.withHeader(ACCEPT, equalTo(APPLICATION_JSON))
		);

		// Reset all the mocks on the WireMockServer
		this.wireMockServer.resetAll();
	}

	private static Stream<Fighters> invalidFighters() {
		return Stream.of(
			new Fighters(),
			new Fighters(DEFAULT_HERO, null),
			new Fighters(null, DEFAULT_VILLAIN),
			new Fighters(new Hero(null, DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), DEFAULT_VILLAIN),
			new Fighters(new Hero("", DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), DEFAULT_VILLAIN),
			new Fighters(new Hero(DEFAULT_HERO_NAME, DEFAULT_HERO_LEVEL, "", DEFAULT_HERO_POWERS), DEFAULT_VILLAIN),
			new Fighters(DEFAULT_HERO, new Villain(null, DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(DEFAULT_HERO, new Villain("", DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(DEFAULT_HERO, new Villain(DEFAULT_VILLAIN_NAME, DEFAULT_VILLAIN_LEVEL, "", DEFAULT_VILLAIN_POWERS))
		);
	}

	private static <T> String writeAsJson(T entity) {
		try {
			return OBJECT_MAPPER.writeValueAsString(entity);
		}
		catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String getDefaultVillainJson() {
		return writeAsJson(DEFAULT_VILLAIN);
	}

	private static String getDefaultHeroJson() {
		return writeAsJson(DEFAULT_HERO);
	}
}
