package io.quarkus.sample.superheroes.fight.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.client.Villain;
import io.quarkus.sample.superheroes.fight.service.FightService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.Uni;

/**
 * Tests the resource layer ({@link FightResource} specifically).
 */
@QuarkusTest
public class FightResourceTests {
	private static final String DEFAULT_FIGHT_ID = new ObjectId().toString();
	private static final Instant DEFAULT_FIGHT_DATE = Instant.now();

	private static final String DEFAULT_HERO_NAME = "Super Baguette";
	private static final String DEFAULT_HERO_PICTURE = "super_baguette.png";
	private static final String DEFAULT_HERO_POWERS = "eats baguette really quickly";
	private static final int DEFAULT_HERO_LEVEL = 42;
	private static final String HEROES_TEAM_NAME = "heroes";

	private static final String DEFAULT_VILLAIN_NAME = "Super Chocolatine";
	private static final String DEFAULT_VILLAIN_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_VILLAIN_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_VILLAIN_LEVEL = 40;
	private static final String VILLAINS_TEAM_NAME = "villains";

	@InjectMock
	FightService fightService;

	@Test
	public void helloEndpoint() {
		get("/api/fights/hello")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(TEXT)
			.body(is("Hello Fight Resource"));

		verifyNoInteractions(this.fightService);
	}

  @Test
  public void helloHeroesEndpoint() {
    when(this.fightService.helloHeroes())
      .thenReturn(Uni.createFrom().item("Hello Hero Resource"));

    get("/api/fights/hello/heroes")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Hero Resource"));

    verify(this.fightService).helloHeroes();
    verifyNoMoreInteractions(this.fightService);
  }

  @Test
  public void helloVillainsEndpoint() {
    when(this.fightService.helloVillains())
      .thenReturn(Uni.createFrom().item("Hello Villains Resource"));

    get("/api/fights/hello/villains")
      .then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Villains Resource"));

    verify(this.fightService).helloVillains();
    verifyNoMoreInteractions(this.fightService);
  }

  @Test
	public void shouldGetRandomFighters() {
		when(this.fightService.findRandomFighters())
			.thenReturn(Uni.createFrom().item(createDefaultFighters()));

		var randomFighters = get("/api/fights/randomfighters")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fighters.class);

    assertThat(randomFighters)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createDefaultFighters());

		verify(this.fightService).findRandomFighters();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetNoFights() {
		when(this.fightService.findAllFights()).thenReturn(Uni.createFrom().item(List.of()));

		get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.body("$.size()", is(0));

		verify(this.fightService).findAllFights();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetAllFights() {
		when(this.fightService.findAllFights())
			.thenReturn(Uni.createFrom().item(List.of(createFightHeroWon())));

		var fights = get("/api/fights")
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().body()
      .jsonPath().getList(".", Fight.class);

      assertThat(fights)
        .singleElement()
        .usingRecursiveComparison()
        .isEqualTo(createFightHeroWon());

		verify(this.fightService).findAllFights();
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetNoFightFound() {
		when(this.fightService.findFightById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/fights/{id}", DEFAULT_FIGHT_ID)
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.fightService).findFightById(eq(DEFAULT_FIGHT_ID));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldGetFight() {
		when(this.fightService.findFightById(eq(DEFAULT_FIGHT_ID)))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = get("/api/fights/{id}", DEFAULT_FIGHT_ID)
			.then()
			.statusCode(OK.getStatusCode())
			.contentType(JSON)
      .extract().as(Fight.class);

    assertThat(fight)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		verify(this.fightService).findFightById(eq(DEFAULT_FIGHT_ID));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldNotPerformFightNullFighters() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/fights")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.fightService);
	}

	@ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
	@MethodSource("invalidFighters")
	public void shouldNotPerformFightInvalidFighters(Fighters fighters) {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(fighters)
				.post("/api/fights")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.fightService);
	}

	@Test
	public void shouldPerformFight() {
		var fightersMatcher = fightersMatcher(createDefaultFighters());

		when(this.fightService.performFight(argThat(fightersMatcher)))
			.thenReturn(Uni.createFrom().item(createFightHeroWon()));

		var fight = given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body(createDefaultFighters())
				.post("/api/fights")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Fight.class);

    assertThat(fight)
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(createFightHeroWon());

		verify(this.fightService).performFight(argThat(fightersMatcher));
		verifyNoMoreInteractions(this.fightService);
	}

	@Test
	public void shouldPingOpenAPI() {
		get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	private static Stream<Fighters> invalidFighters() {
		return Stream.of(
			new Fighters(),
			new Fighters(createDefaultHero(), null),
			new Fighters(null, createDefaultVillain()),
			new Fighters(new Hero(null, DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(new Hero("", DEFAULT_HERO_LEVEL, DEFAULT_HERO_PICTURE, DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(new Hero(DEFAULT_HERO_NAME, DEFAULT_HERO_LEVEL, "", DEFAULT_HERO_POWERS), createDefaultVillain()),
			new Fighters(createDefaultHero(), new Villain(null, DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(createDefaultHero(), new Villain("", DEFAULT_VILLAIN_LEVEL, DEFAULT_VILLAIN_PICTURE, DEFAULT_VILLAIN_POWERS)),
			new Fighters(createDefaultHero(), new Villain(DEFAULT_VILLAIN_NAME, DEFAULT_VILLAIN_LEVEL, "", DEFAULT_VILLAIN_POWERS))
		);
	}

	private static Hero createDefaultHero() {
		return new Hero(
			DEFAULT_HERO_NAME,
			DEFAULT_HERO_LEVEL,
			DEFAULT_HERO_PICTURE,
			DEFAULT_HERO_POWERS
		);
	}

	private static Villain createDefaultVillain() {
		return new Villain(
			DEFAULT_VILLAIN_NAME,
			DEFAULT_VILLAIN_LEVEL,
			DEFAULT_VILLAIN_PICTURE,
			DEFAULT_VILLAIN_POWERS
		);
	}

	private static Fighters createDefaultFighters() {
		return new Fighters(createDefaultHero(), createDefaultVillain());
	}

	private static Fight createFightHeroWon() {
		var fight = new Fight();
		fight.id = new ObjectId(DEFAULT_FIGHT_ID);
		fight.fightDate = DEFAULT_FIGHT_DATE;
		fight.winnerName = DEFAULT_HERO_NAME;
		fight.winnerLevel = DEFAULT_HERO_LEVEL;
		fight.winnerPicture = DEFAULT_HERO_PICTURE;
		fight.loserName = DEFAULT_VILLAIN_NAME;
		fight.loserLevel = DEFAULT_VILLAIN_LEVEL;
		fight.loserPicture = DEFAULT_VILLAIN_PICTURE;
		fight.winnerTeam = HEROES_TEAM_NAME;
		fight.loserTeam = VILLAINS_TEAM_NAME;

		return fight;
	}

	private static ArgumentMatcher<Hero> heroMatcher(Hero hero) {
		return h -> (hero == h) || (
			(hero != null) &&
				(h != null) &&
				Objects.equals(hero.getName(), h.getName()) &&
				Objects.equals(hero.getLevel(), h.getLevel()) &&
				Objects.equals(hero.getPicture(), h.getPicture()) &&
				Objects.equals(hero.getPowers(), h.getPowers())
		);
	}

	private static ArgumentMatcher<Villain> villainMatcher(Villain villain) {
		return v -> (villain == v) || (
			(villain != null) &&
				(v != null) &&
				Objects.equals(villain.getName(), v.getName()) &&
				Objects.equals(villain.getLevel(), v.getLevel()) &&
				Objects.equals(villain.getPicture(), v.getPicture()) &&
				Objects.equals(villain.getPowers(), v.getPowers())
		);
	}

	private static ArgumentMatcher<Fighters> fightersMatcher(Fighters fighters) {
		return f -> (fighters == f) || (
			(fighters != null) &&
				(f != null) &&
				heroMatcher(f.getHero()).matches(fighters.getHero()) &&
				villainMatcher(f.getVillain()).matches(fighters.getVillain())
		);
	}
}
