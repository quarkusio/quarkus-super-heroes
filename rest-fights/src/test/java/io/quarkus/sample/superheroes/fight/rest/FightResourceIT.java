package io.quarkus.sample.superheroes.fight.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.ParameterizedTest.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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
import io.quarkus.sample.superheroes.fight.InjectKafkaConsumer;
import io.quarkus.sample.superheroes.fight.InjectWireMock;
import io.quarkus.sample.superheroes.fight.KafkaConsumerResource;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Integration tests for the application as a whole. Orders tests in an order to faciliate a scenario of interactions
 * <p>
 *   Uses wiremock to stub responses and verify interactions with the hero and villain services.
 * </p>
 * <p>
 *   Uses an external container image for Kafka
 * </p>
 * @see HeroesVillainsWiremockServerResource
 * @see io.quarkus.sample.superheroes.fight.KafkaConsumerResource
 */
@QuarkusIntegrationTest
@QuarkusTestResource(HeroesVillainsWiremockServerResource.class)
@QuarkusTestResource(value = KafkaConsumerResource.class, restrictToAnnotatedClass = true)
@TestMethodOrder(OrderAnnotation.class)
public class FightResourceIT {
	private static final int DEFAULT_ORDER = 0;

	private static final String HERO_API_URI = "/api/heroes/random";
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

	private static final String VILLAIN_API_URI = "/api/villains/random";
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

	@InjectKafkaConsumer
	KafkaConsumer<String, Fight> fightsConsumer;

	@BeforeAll
	public static void beforeAll() {
		OBJECT_MAPPER.setSerializationInclusion(Include.NON_EMPTY);
	}

	@BeforeEach
	public void beforeEach() {
		// Reset WireMock
		this.wireMockServer.resetAll();
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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(FALLBACK_HERO.getName()),
				"hero.level", is(FALLBACK_HERO.getLevel()),
				"hero.picture", is(FALLBACK_HERO.getPicture()),
				"hero.powers", is(FALLBACK_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(DEFAULT_VILLAIN.getName()),
				"villain.level", is(DEFAULT_VILLAIN.getLevel()),
				"villain.picture", is(DEFAULT_VILLAIN.getPicture()),
				"villain.powers", is(DEFAULT_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(DEFAULT_HERO.getName()),
				"hero.level", is(DEFAULT_HERO.getLevel()),
				"hero.picture", is(DEFAULT_HERO.getPicture()),
				"hero.powers", is(DEFAULT_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(FALLBACK_VILLAIN.getName()),
				"villain.level", is(FALLBACK_VILLAIN.getLevel()),
				"villain.picture", is(FALLBACK_VILLAIN.getPicture()),
				"villain.powers", is(FALLBACK_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(FALLBACK_HERO.getName()),
				"hero.level", is(FALLBACK_HERO.getLevel()),
				"hero.picture", is(FALLBACK_HERO.getPicture()),
				"hero.powers", is(FALLBACK_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(DEFAULT_VILLAIN.getName()),
				"villain.level", is(DEFAULT_VILLAIN.getLevel()),
				"villain.picture", is(DEFAULT_VILLAIN.getPicture()),
				"villain.powers", is(DEFAULT_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(DEFAULT_HERO.getName()),
				"hero.level", is(DEFAULT_HERO.getLevel()),
				"hero.picture", is(DEFAULT_HERO.getPicture()),
				"hero.powers", is(DEFAULT_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(FALLBACK_VILLAIN.getName()),
				"villain.level", is(FALLBACK_VILLAIN.getLevel()),
				"villain.picture", is(FALLBACK_VILLAIN.getPicture()),
				"villain.powers", is(FALLBACK_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(FALLBACK_HERO.getName()),
				"hero.level", is(FALLBACK_HERO.getLevel()),
				"hero.picture", is(FALLBACK_HERO.getPicture()),
				"hero.powers", is(FALLBACK_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(FALLBACK_VILLAIN.getName()),
				"villain.level", is(FALLBACK_VILLAIN.getLevel()),
				"villain.picture", is(FALLBACK_VILLAIN.getPicture()),
				"villain.powers", is(FALLBACK_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(FALLBACK_HERO.getName()),
				"hero.level", is(FALLBACK_HERO.getLevel()),
				"hero.picture", is(FALLBACK_HERO.getPicture()),
				"hero.powers", is(FALLBACK_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(FALLBACK_VILLAIN.getName()),
				"villain.level", is(FALLBACK_VILLAIN.getLevel()),
				"villain.picture", is(FALLBACK_VILLAIN.getPicture()),
				"villain.powers", is(FALLBACK_VILLAIN.getPowers())
			);

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

		get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"$", notNullValue(),
				"hero", notNullValue(),
				"hero.name", is(DEFAULT_HERO.getName()),
				"hero.level", is(DEFAULT_HERO.getLevel()),
				"hero.picture", is(DEFAULT_HERO.getPicture()),
				"hero.powers", is(DEFAULT_HERO.getPowers()),
				"villain", notNullValue(),
				"villain.name", is(DEFAULT_VILLAIN.getName()),
				"villain.level", is(DEFAULT_VILLAIN.getLevel()),
				"villain.picture", is(DEFAULT_VILLAIN.getPicture()),
				"villain.powers", is(DEFAULT_VILLAIN.getPowers())
			);

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
	public void getAllFights() {
		getAndVerifyAllFights();
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void getFightNotFound() {
		get("/api/fights/{id}", -1)
			.then().statusCode(NOT_FOUND.getStatusCode());
	}

	@Test
	@Order(DEFAULT_ORDER)
	public void getFoundFight() {
		get("/api/fights/{id}", getAndVerifyAllFights().get(0).id)
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"winnerName", is("Chewbacca"),
				"winnerLevel", is(5),
				"winnerPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg"),
				"winnerTeam", is(HEROES_TEAM_NAME),
				"loserName", is("Wanderer"),
				"loserLevel", is(3),
				"loserPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg"),
				"loserTeam", is(VILLAINS_TEAM_NAME)
			);
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
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(new Fighters(DEFAULT_HERO, DEFAULT_VILLAIN))
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body(
					"winnerName", is(DEFAULT_HERO.getName()),
					"winnerLevel", is(DEFAULT_HERO.getLevel()),
					"winnerPicture", is(DEFAULT_HERO.getPicture()),
					"winnerTeam", is(HEROES_TEAM_NAME),
					"loserName", is(DEFAULT_VILLAIN.getName()),
					"loserLevel", is(DEFAULT_VILLAIN.getLevel()),
					"loserPicture", is(DEFAULT_VILLAIN.getPicture()),
					"loserTeam", is(VILLAINS_TEAM_NAME)
				);

		get("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body("size()", is(NB_FIGHTS + 1));

		var records = this.fightsConsumer.poll(Duration.ofSeconds(10)).records("fights");
		var fight = StreamSupport.stream(records.spliterator(), false)
			.map(ConsumerRecord::value)
			.findFirst();

		assertThat(fight)
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"winnerName",
				"winnerLevel",
				"winnerPicture",
				"winnerTeam",
				"loserName",
				"loserLevel",
				"loserPicture",
				"loserTeam"
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

		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(fighters)
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body(
					"winnerName", is(DEFAULT_VILLAIN.getName()),
					"winnerLevel", is(DEFAULT_HERO.getLevel()),
					"winnerPicture", is(DEFAULT_VILLAIN.getPicture()),
					"winnerTeam", is(VILLAINS_TEAM_NAME),
					"loserName", is(DEFAULT_HERO.getName()),
					"loserLevel", is(DEFAULT_VILLAIN.getLevel()),
					"loserPicture", is(DEFAULT_HERO.getPicture()),
					"loserTeam", is(HEROES_TEAM_NAME)
				);

		get("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
				.body("size()", is(NB_FIGHTS + 2));

		var records = this.fightsConsumer.poll(Duration.ofSeconds(10)).records("fights");
		var fight = StreamSupport.stream(records.spliterator(), false)
			.map(ConsumerRecord::value)
			.findFirst();

		assertThat(fight)
			.isNotNull()
			.isPresent()
			.get()
			.extracting(
				"winnerName",
				"winnerLevel",
				"winnerPicture",
				"winnerTeam",
				"loserName",
				"loserLevel",
				"loserPicture",
				"loserTeam"
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
		return get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
			.body(
				"size()", is(NB_FIGHTS),
				"[0].winnerName", is("Chewbacca"),
				"[0].winnerLevel", is(5),
				"[0].winnerPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg"),
				"[0].winnerTeam", is(HEROES_TEAM_NAME),
				"[0].loserName", is("Wanderer"),
				"[0].loserLevel", is(3),
				"[0].loserPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg"),
				"[0].loserTeam", is(VILLAINS_TEAM_NAME),
				"[1].winnerName", is("Galadriel"),
				"[1].winnerLevel", is(10),
				"[1].winnerPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/galadriel--1293733805363806029.jpg"),
				"[1].winnerTeam", is(HEROES_TEAM_NAME),
				"[1].loserName", is("Darth Vader"),
				"[1].loserLevel", is(8),
				"[1].loserPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg"),
				"[1].loserTeam", is(VILLAINS_TEAM_NAME),
				"[2].winnerName", is("Annihilus"),
				"[2].winnerLevel", is(23),
				"[2].winnerPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/annihilus--751928780106678215.jpg"),
				"[2].winnerTeam", is(VILLAINS_TEAM_NAME),
				"[2].loserName", is("Shikamaru"),
				"[2].loserLevel", is(1),
				"[2].loserPicture", is("https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/nara-shikamaru-1970614934047311432.jpg"),
				"[2].loserTeam", is(HEROES_TEAM_NAME)
			)
			.extract()
			.body().jsonPath().getList(".", Fight.class);
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
			.forEach(i ->
				get("/api/fights/randomfighters")
					.then()
					.statusCode(OK.getStatusCode())
					.contentType(JSON)
					.body(
						"$", notNullValue(),
						"hero", notNullValue(),
						"hero.name", is(DEFAULT_HERO.getName()),
						"hero.level", is(DEFAULT_HERO.getLevel()),
						"hero.picture", is(DEFAULT_HERO.getPicture()),
						"hero.powers", is(DEFAULT_HERO.getPowers()),
						"villain", notNullValue(),
						"villain.name", is(DEFAULT_VILLAIN.getName()),
						"villain.level", is(DEFAULT_VILLAIN.getLevel()),
						"villain.picture", is(DEFAULT_VILLAIN.getPicture()),
						"villain.powers", is(DEFAULT_VILLAIN.getPowers())
					)
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
