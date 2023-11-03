package io.quarkus.sample.superheroes.fight.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.HttpHeaders.*;
import static jakarta.ws.rs.core.MediaType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.grpcmock.GrpcMock.*;
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
import org.grpcmock.GrpcMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.GrpcMockServerResource;
import io.quarkus.sample.superheroes.fight.HeroesVillainsNarrationWiremockServerResource;
import io.quarkus.sample.superheroes.fight.InjectGrpcMock;
import io.quarkus.sample.superheroes.fight.InjectWireMock;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate.FightToNarrateLocation;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.location.grpc.HelloReply;
import io.quarkus.sample.superheroes.location.grpc.HelloRequest;
import io.quarkus.sample.superheroes.location.grpc.Location;
import io.quarkus.sample.superheroes.location.grpc.LocationType;
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc;
import io.quarkus.sample.superheroes.location.grpc.RandomLocationRequest;
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
import io.grpc.Status;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.Vertx;

/**
 * Integration tests for the application as a whole. Orders tests in an order to faciliate a scenario of interactions
 * <p>
 *   Uses wiremock to stub responses and verify interactions with the hero, villain, and narration services.
 * </p>
 * <p>
 *   Uses grpcmock to stub responses and verify interactions with the location service
 * </p>
 * <p>
 *   Uses an external container image for Kafka
 * </p>
 * @see HeroesVillainsNarrationWiremockServerResource
 * @see GrpcMockServerResource
 * @see KafkaCompanionResource
 */
@QuarkusIntegrationTest
@QuarkusTestResource(value = HeroesVillainsNarrationWiremockServerResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = KafkaCompanionResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = GrpcMockServerResource.class, restrictToAnnotatedClass = true)
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

  private static final String FALLBACK_LOCATION_NAME = "Fallback location";
  private static final String FALLBACK_LOCATION_DESCRIPTION = "This is a fallback location. Not generally someplace you'd like to visit.";
  private static final String FALLBACK_LOCATION_PICTURE = "https://dummyimage.com/280x380/b22222/ffffff&text=Fallback+Location";
  private static final FightLocation FALLBACK_LOCATION = new FightLocation(FALLBACK_LOCATION_NAME, FALLBACK_LOCATION_DESCRIPTION, FALLBACK_LOCATION_PICTURE);

  private static final String NARRATION_API_BASE_URI = "/api/narration";
  private static final String NARRATION_API_HELLO_URI = NARRATION_API_BASE_URI + "/hello";
  private static final String DEFAULT_NARRATION = """
                                                  This is a default narration - NOT a fallback!
                                                  
                                                  High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                  With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                  
                                                  In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                  The people knew that their protector had once again ensured their safety.
                                                  """;
  private static final String FALLBACK_NARRATION = """
                                                   High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                                   With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                                   
                                                   In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                                   The people knew that their protector had once again ensured their safety.
                                                   """;

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

  private static final FightLocation DEFAULT_LOCATION = new FightLocation(
    "Gotham City",
    "An American city rife with corruption and crime, the home of its iconic protector Batman.",
    "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg"
  );

  private static final Location DEFAULT_GRPC_LOCATION = Location.newBuilder()
    .setName(DEFAULT_LOCATION.name())
    .setDescription(DEFAULT_LOCATION.description())
    .setPicture(DEFAULT_LOCATION.picture())
    .setType(LocationType.PLANET)
    .build();

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final int NB_FIGHTS = 3;

	@InjectWireMock
	WireMockServer wireMockServer;

  @InjectGrpcMock
  GrpcMock grpcMock;

	@InjectKafkaCompanion
  KafkaCompanion companion;

  private static Vertx vertx;

	@BeforeAll
	public static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

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

    // Reset GrpcMock
    this.grpcMock.resetAll();

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
		resetHeroVillainCircuitBreakersToClosedState();

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
	public void getRandomFightersHeroDelay() {
		resetHeroVillainCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(
					okForContentType(APPLICATION_JSON, getDefaultHeroJson())
						.withFixedDelay(3_000)
				)
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
	public void getRandomFightersVillainFallback() {
		resetHeroVillainCircuitBreakersToClosedState();

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
	public void getRandomFightersVillainDelay() {
		resetHeroVillainCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(okForContentType(APPLICATION_JSON, getDefaultHeroJson()))
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(
					okForContentType(APPLICATION_JSON, getDefaultVillainJson())
					.withFixedDelay(3_000)
				)
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
		resetHeroVillainCircuitBreakersToClosedState();

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
		resetHeroVillainCircuitBreakersToClosedState();

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
  @Order(DEFAULT_ORDER)
  public void shouldGetNarration() {
    this.wireMockServer.stubFor(
      WireMock.post(urlEqualTo(NARRATION_API_BASE_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON))
        .willReturn(okForContentType(TEXT_PLAIN, DEFAULT_NARRATION))
    );

    given()
      .accept(TEXT)
      .contentType(JSON)
      .body(createFightToNarrateHeroWon())
      .when().post("/api/fights/narrate").then()
      .contentType(TEXT)
      .body(is(DEFAULT_NARRATION));

    this.wireMockServer.verify(postRequestedFor(urlEqualTo(NARRATION_API_BASE_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void getNarrationFallback() {
    resetNarrationCircuitBreakersToClosedState();

    this.wireMockServer.stubFor(
      WireMock.post(urlEqualTo(NARRATION_API_BASE_URI))
        .willReturn(serverError())
    );

    given()
      .accept(TEXT)
      .contentType(JSON)
      .body(createFightToNarrateHeroWon())
      .when().post("/api/fights/narrate").then()
      .contentType(TEXT)
      .body(is(FALLBACK_NARRATION));

    this.wireMockServer.verify(4,
      postRequestedFor(urlEqualTo(NARRATION_API_BASE_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON))
    );
  }

	@Test
  @Order(DEFAULT_ORDER + 1)
  public void getNarrationDelay() {
    resetNarrationCircuitBreakersToClosedState();

		var delay = 11_000;

    this.wireMockServer.stubFor(
      WireMock.post(urlEqualTo(NARRATION_API_BASE_URI))
        .willReturn(
					okForContentType(TEXT_PLAIN, DEFAULT_NARRATION)
						.withFixedDelay(delay)
        )
    );

		// Need to increase the rest-assured timeouts
		var config = RestAssured.config()
			.httpClient(
				HttpClientConfig.httpClientConfig()
					.setParam("http.connection.timeout", delay * 4)
					.setParam("http.socket.timeout", delay * 4)
			);

    given()
	    .config(config)
      .accept(TEXT)
      .contentType(JSON)
      .body(createFightToNarrateHeroWon())
      .when().post("/api/fights/narrate").then()
        .contentType(TEXT)
        .body(is(FALLBACK_NARRATION));

    this.wireMockServer.verify(4,
      postRequestedFor(urlEqualTo(NARRATION_API_BASE_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON))
    );
  }

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersHeroAndVillainFallback() {
		resetHeroVillainCircuitBreakersToClosedState();

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
	public void getRandomFightersHeroAndVillainDelay() {
		resetHeroVillainCircuitBreakersToClosedState();

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(HERO_API_URI))
				.willReturn(
					okForContentType(APPLICATION_JSON, getDefaultHeroJson())
						.withFixedDelay(3_000)
				)
		);

		this.wireMockServer.stubFor(
			WireMock.get(urlEqualTo(VILLAIN_API_URI))
				.willReturn(
					okForContentType(APPLICATION_JSON, getDefaultVillainJson())
					.withFixedDelay(3_000)
				)
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
  public void getRandomLocationOk() {
    resetLocationCircuitBreakerToClosedState();

    this.grpcMock.register(
      unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
        .willReturn(DEFAULT_GRPC_LOCATION)
    );

    var randomLocation = get("/api/fights/randomlocation")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON)
      .extract().as(FightLocation.class);

    assertThat(randomLocation)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(DEFAULT_LOCATION);

    this.grpcMock.verifyThat(
      calledMethod(LocationsGrpc.getGetRandomLocationMethod())
        .withRequest(RandomLocationRequest.newBuilder().build())
        .build(),
      GrpcMock.times(1)
    );
  }

	@Test
  @Order(DEFAULT_ORDER + 1)
  public void getRandomLocationDelay() {
    resetLocationCircuitBreakerToClosedState();

    this.grpcMock.register(
      unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
        .willReturn(
					response(DEFAULT_GRPC_LOCATION)
						.withFixedDelay(Duration.ofSeconds(3))
        )
    );

    var randomLocation = get("/api/fights/randomlocation")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON)
      .extract().as(FightLocation.class);

    assertThat(randomLocation)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(FALLBACK_LOCATION);

    this.grpcMock.verifyThat(
      calledMethod(LocationsGrpc.getGetRandomLocationMethod())
        .withRequest(RandomLocationRequest.newBuilder().build())
        .build(),
      GrpcMock.times(1)
    );
  }

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @MethodSource("randomLocationFailReasons")
  @Order(DEFAULT_ORDER + 1)
  public void getRandomLocationFail(Status status, int expectedNumberOfCalls) {
    resetLocationCircuitBreakerToClosedState();

    this.grpcMock.register(
      unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
        .willReturn(status)
    );

    var randomLocation = get("/api/fights/randomlocation")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON)
      .extract().as(FightLocation.class);

    assertThat(randomLocation)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(FALLBACK_LOCATION);

    this.grpcMock.verifyThat(
      calledMethod(LocationsGrpc.getGetRandomLocationMethod())
        .withRequest(RandomLocationRequest.newBuilder().build())
        .build(),
      GrpcMock.times(expectedNumberOfCalls)
    );
  }

  static Stream<Arguments> randomLocationFailReasons() {
    return Stream.of(
      Arguments.of(Status.UNAVAILABLE.withDescription("Service isn't there"), 4),
      Arguments.of(Status.NOT_FOUND.withDescription("A location was not found"), 1)
    );
  }

	@Test
	@Order(DEFAULT_ORDER + 1)
	public void getRandomFightersAllOk() {
		resetHeroVillainCircuitBreakersToClosedState();

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
  @Order(DEFAULT_ORDER)
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
  @Order(DEFAULT_ORDER)
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
  public void helloNarrationOk() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(NARRATION_API_HELLO_URI))
        .willReturn(okForContentType(TEXT_PLAIN, "Hello narration!"))
    );

    get("/api/fights/hello/narration")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello narration!"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(NARRATION_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER)
  public void helloLocationsOk() {
    this.grpcMock.register(
			unaryMethod(LocationsGrpc.getHelloMethod())
				.willReturn(HelloReply.newBuilder().setMessage("Hello location!").build())
		);

    get("/api/fights/hello/locations")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello location!"));

    this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getHelloMethod())
				.withRequest(HelloRequest.newBuilder().build())
				.build(),
			times(1)
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloLocationsFallback() {
    this.grpcMock.register(
      unaryMethod(LocationsGrpc.getHelloMethod())
        .willReturn(
          response(HelloReply.newBuilder().setMessage("Hello location!").build())
            .withFixedDelay(Duration.ofSeconds(6))
        )
    );

    get("/api/fights/hello/locations")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Locations microservice"));

    this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getHelloMethod())
				.withRequest(HelloRequest.newBuilder().build())
				.build(),
			times(1)
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloLocationsFail() {
    this.grpcMock.register(
			unaryMethod(LocationsGrpc.getHelloMethod())
				.willReturn(Status.UNAVAILABLE.withDescription("Service isn't there"))
		);

    get("/api/fights/hello/locations")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Locations microservice"));

   this.grpcMock.verifyThat(
			calledMethod(LocationsGrpc.getHelloMethod())
				.withRequest(HelloRequest.newBuilder().build())
				.build(),
			times(1)
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloNarrationFallback() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(NARRATION_API_HELLO_URI))
        .willReturn(
          okForContentType(TEXT_PLAIN, "Hello narration!")
            .withFixedDelay(6 * 1000)
        )
    );

    get("/api/fights/hello/narration")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Narration microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(NARRATION_API_HELLO_URI))
        .withHeader(ACCEPT, containing(TEXT_PLAIN))
    );
  }

  @Test
  @Order(DEFAULT_ORDER + 1)
  public void helloNarrationFail() {
    this.wireMockServer.stubFor(
      WireMock.get(urlEqualTo(NARRATION_API_HELLO_URI))
        .willReturn(serverError())
    );

    get("/api/fights/hello/narration")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Could not invoke the Narration microservice"));

    this.wireMockServer.verify(1,
      getRequestedFor(urlEqualTo(NARRATION_API_HELLO_URI))
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
    expectedFight.winnerPowers = DEFAULT_HERO_POWERS;
    expectedFight.winnerTeam = HEROES_TEAM_NAME;
    expectedFight.loserName = "Wanderer";
    expectedFight.loserLevel = 3;
    expectedFight.loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg";
    expectedFight.loserPowers = DEFAULT_VILLAIN_POWERS;
    expectedFight.loserTeam = VILLAINS_TEAM_NAME;
    expectedFight.location = DEFAULT_LOCATION;

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

	@Test
  @Order(DEFAULT_ORDER)
	public void performFightInvalidFighters() {
    invalidFighters().forEach(fighters ->
      given()
        .when()
          .contentType(JSON)
          .accept(JSON)
          .body(fighters)
          .post("/api/fights")
        .then()
          .statusCode(BAD_REQUEST.getStatusCode())
    );
	}

  // This is written as an @Test instead of @ParameterizedTest
  // because @MethodSource does not like java records for some reason
  @Test
  @Order(DEFAULT_ORDER)
  public void shouldNotGetNarrationBecauseInvalidFight() {
    invalidFightsToNarrate().forEach(fight -> {
      given()
        .accept(TEXT)
        .contentType(JSON)
        .body(fight)
        .when().post("/api/fights/narrate").then()
        .statusCode(BAD_REQUEST.getStatusCode());
    });
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
    expectedFight.winnerName = DEFAULT_HERO.name();
    expectedFight.winnerLevel = DEFAULT_HERO.level();
    expectedFight.winnerPicture = DEFAULT_HERO.picture();
    expectedFight.winnerTeam = HEROES_TEAM_NAME;
    expectedFight.winnerPowers = DEFAULT_HERO.powers();
    expectedFight.loserName = DEFAULT_VILLAIN.name();
    expectedFight.loserLevel = DEFAULT_VILLAIN.level();
    expectedFight.loserPicture = DEFAULT_VILLAIN.picture();
    expectedFight.loserPowers = DEFAULT_VILLAIN.powers();
    expectedFight.loserTeam = VILLAINS_TEAM_NAME;
    expectedFight.location = DEFAULT_LOCATION;

		var fightResult = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(new FightRequest(DEFAULT_HERO, DEFAULT_VILLAIN, DEFAULT_LOCATION))
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
				DEFAULT_HERO.name(),
				DEFAULT_HERO.level(),
				DEFAULT_HERO.picture(),
				HEROES_TEAM_NAME,
				DEFAULT_VILLAIN.name(),
				DEFAULT_VILLAIN.level(),
				DEFAULT_VILLAIN.picture(),
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

    var fightRequest = new FightRequest(
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
      ),
      DEFAULT_LOCATION
		);

    var expectedFight = new Fight();
    expectedFight.loserName = DEFAULT_HERO.name();
    expectedFight.loserLevel = DEFAULT_VILLAIN.level();
    expectedFight.loserPicture = DEFAULT_HERO.picture();
    expectedFight.loserPowers = DEFAULT_HERO.powers();
    expectedFight.loserTeam = HEROES_TEAM_NAME;
    expectedFight.winnerName = DEFAULT_VILLAIN.name();
    expectedFight.winnerLevel = DEFAULT_HERO.level();
    expectedFight.winnerPicture = DEFAULT_VILLAIN.picture();
    expectedFight.winnerPowers = DEFAULT_VILLAIN.powers();
    expectedFight.winnerTeam = VILLAINS_TEAM_NAME;
    expectedFight.location = DEFAULT_LOCATION;

		var fightResult = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(fightRequest)
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
				DEFAULT_VILLAIN.name(),
				DEFAULT_HERO.level(),
				DEFAULT_VILLAIN.picture(),
				VILLAINS_TEAM_NAME,
				DEFAULT_HERO.name(),
				DEFAULT_VILLAIN.level(),
				DEFAULT_HERO.picture(),
				HEROES_TEAM_NAME
			);
	}

	private List<Fight> getAndVerifyAllFights() {
    var expectedFights = List.of(new Fight(), new Fight(), new Fight());
    expectedFights.get(0).winnerName = "Chewbacca";
    expectedFights.get(0).winnerLevel = 5;
    expectedFights.get(0).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg";
    expectedFights.get(0).winnerPowers = DEFAULT_HERO_POWERS;
    expectedFights.get(0).winnerTeam = HEROES_TEAM_NAME;
    expectedFights.get(0).loserName = "Wanderer";
    expectedFights.get(0).loserLevel = 3;
    expectedFights.get(0).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg";
    expectedFights.get(0).loserPowers = DEFAULT_VILLAIN_POWERS;
    expectedFights.get(0).loserTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(0).location = DEFAULT_LOCATION;
    expectedFights.get(1).winnerName = "Galadriel";
    expectedFights.get(1).winnerLevel = 10;
    expectedFights.get(1).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/galadriel--1293733805363806029.jpg";
    expectedFights.get(1).winnerTeam = HEROES_TEAM_NAME;
    expectedFights.get(1).winnerPowers = DEFAULT_HERO_POWERS;
    expectedFights.get(1).loserName = "Darth Vader";
    expectedFights.get(1).loserLevel = 8;
    expectedFights.get(1).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg";
    expectedFights.get(1).loserPowers = DEFAULT_VILLAIN_POWERS;
    expectedFights.get(1).loserTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(1).location = new FightLocation("Krypton", "An ancient world, Krypton was home to advanced civilization known as Kryptonians.", "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/krypton.jpg");
    expectedFights.get(2).winnerName = "Annihilus";
    expectedFights.get(2).winnerLevel = 23;
    expectedFights.get(2).winnerPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/annihilus--751928780106678215.jpg";
    expectedFights.get(2).winnerPowers = DEFAULT_VILLAIN_POWERS;
    expectedFights.get(2).winnerTeam = VILLAINS_TEAM_NAME;
    expectedFights.get(2).loserName = "Shikamaru";
    expectedFights.get(2).loserLevel = 1;
    expectedFights.get(2).loserPicture = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/nara-shikamaru-1970614934047311432.jpg";
    expectedFights.get(2).loserPowers = DEFAULT_HERO_POWERS;
    expectedFights.get(2).loserTeam = HEROES_TEAM_NAME;
    expectedFights.get(2).location = new FightLocation("Earth", "Earth, our home planet, is the only place we know of so far that is inhabited by living things.", "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/earth.jpg");

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
	private void resetHeroVillainCircuitBreakersToClosedState() {
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

  private void resetLocationCircuitBreakerToClosedState() {
		try {
			// Sleep the necessary delay duration for the breaker to moved into the half-open position
			TimeUnit.SECONDS.sleep(2);
		}
		catch (InterruptedException ex) {
			Log.error(ex.getMessage(), ex);
		}

		// Reset all the mocks on the GrpcMock
		this.grpcMock.resetAll();

		// Stub successful requests
    this.grpcMock.register(
      unaryMethod(LocationsGrpc.getGetRandomLocationMethod())
        .willReturn(DEFAULT_GRPC_LOCATION)
    );

		// The circuit breaker requestVolumeThreshold == 8, so we need to make n+1 successful requests for it to clear
		IntStream.rangeClosed(0, 8)
			.forEach(i -> {
        var location = get("/api/fights/randomlocation").then()
          .statusCode(OK.getStatusCode())
          .contentType(JSON)
          .extract().as(FightLocation.class);

        assertThat(location)
          .isNotNull()
          .usingRecursiveComparison()
          .isEqualTo(DEFAULT_LOCATION);
        }
			);

		// Verify successful requests
    this.grpcMock.verifyThat(
      calledMethod(LocationsGrpc.getGetRandomLocationMethod())
        .withRequest(RandomLocationRequest.newBuilder().build())
        .build(),
      GrpcMock.times(9)
    );

		// Reset all the mocks on the GrpcMock
		this.grpcMock.resetAll();
	}

  private void resetNarrationCircuitBreakersToClosedState() {
		try {
			// Sleep the necessary delay duration for the breaker to moved into the half-open position
			TimeUnit.SECONDS.sleep(10);
		}
		catch (InterruptedException ex) {
			Log.error(ex.getMessage(), ex);
		}

		// Reset all the mocks on the WireMockServer
		this.wireMockServer.resetAll();

		// Stub successful requests
    this.wireMockServer.stubFor(
      WireMock.post(urlEqualTo(NARRATION_API_BASE_URI))
        .willReturn(okForContentType(TEXT_PLAIN, DEFAULT_NARRATION))
    );

    var fight = createFightToNarrateHeroWon();

		// The circuit breaker requestVolumeThreshold == 8, so we need to make n+1 successful requests for it to clear
		IntStream.rangeClosed(0, 8)
			.forEach(i -> {
        var narration = given()
          .accept(TEXT)
          .contentType(JSON)
          .body(fight)
          .when().post("/api/fights/narrate").then()
          .statusCode(OK.getStatusCode())
          .contentType(TEXT_PLAIN)
          .extract().body().asString();

        assertThat(narration)
          .isNotNull()
          .isEqualTo(DEFAULT_NARRATION);
        }
			);

		// Verify successful requests
		this.wireMockServer.verify(9,
			postRequestedFor(urlEqualTo(NARRATION_API_BASE_URI))
				.withHeader(ACCEPT, containing(TEXT_PLAIN))
        .withHeader(CONTENT_TYPE, containing(APPLICATION_JSON))
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

  private static Stream<FightToNarrate> invalidFightsToNarrate() {
    return Stream.of(
      new FightToNarrate(null, null, null, 0, null, null, null, 0, null),
      new FightToNarrate("", "", "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, "", "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, "", 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, "", "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, "", "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, DEFAULT_VILLAIN_NAME, "", 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, "", "", 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, null, 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, null, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, null, null, 0, null),
      new FightToNarrate(HEROES_TEAM_NAME, DEFAULT_HERO_NAME, DEFAULT_HERO_POWERS, 0, VILLAINS_TEAM_NAME, DEFAULT_VILLAIN_NAME, null, 0, null)
    );
  }

  private static FightToNarrate createFightToNarrateHeroWon() {
    return new FightToNarrate(
      HEROES_TEAM_NAME,
      DEFAULT_HERO_NAME,
      DEFAULT_HERO_POWERS,
      DEFAULT_HERO_LEVEL,
      VILLAINS_TEAM_NAME,
      DEFAULT_VILLAIN_NAME,
      DEFAULT_VILLAIN_POWERS,
      DEFAULT_VILLAIN_LEVEL,
      new FightToNarrateLocation(DEFAULT_LOCATION.name(), DEFAULT_LOCATION.description())
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

  private static String getDefaultLocationJson() {
    return writeAsJson(DEFAULT_LOCATION);
  }
}
