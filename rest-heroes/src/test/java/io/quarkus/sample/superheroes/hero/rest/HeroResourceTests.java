package io.quarkus.sample.superheroes.hero.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.service.HeroService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.mutiny.Uni;

@QuarkusTest
public class HeroResourceTests {
	private static final String DEFAULT_NAME = "Super Chocolatine";
	private static final String UPDATED_NAME = DEFAULT_NAME + " (updated)";
	private static final String DEFAULT_OTHER_NAME = "Super Chocolatine chocolate in";
	private static final String UPDATED_OTHER_NAME = DEFAULT_OTHER_NAME + " (updated)";
	private static final String DEFAULT_PICTURE = "super_chocolatine.png";
	private static final String UPDATED_PICTURE = "super_chocolatine_updated.png";
	private static final String DEFAULT_POWERS = "does not eat pain au chocolat";
	private static final String UPDATED_POWERS = DEFAULT_POWERS + " (updated)";
	private static final int DEFAULT_LEVEL = 42;
	private static final int UPDATED_LEVEL = DEFAULT_LEVEL + 1;
	private static final long DEFAULT_ID = 1;

	@InjectMock
	HeroService heroService;

	@Test
	public void helloEndpoint() {
		get("/api/heroes/hello")
			.then()
				.statusCode(200)
				.body(is("Hello Hero Resource"));

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldNotGetUnknownHero() {
		when(this.heroService.findHeroById(eq(DEFAULT_ID)))
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/heroes/{id}", DEFAULT_ID)
			.then()
			.statusCode(NOT_FOUND.getStatusCode());

		verify(this.heroService).findHeroById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldGetRandomHeroNotFound() {
		when(this.heroService.findRandomHero())
			.thenReturn(Uni.createFrom().nullItem());

		get("/api/heroes/random")
			.then()
			.statusCode(NOT_FOUND.getStatusCode());

		verify(this.heroService).findRandomHero();
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldGetRandomHeroFound() {
		when(this.heroService.findRandomHero())
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		var hero = get("/api/heroes/random")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Hero.class);

    assertThat(hero)
      .isNotNull()
      .extracting(
        Hero::getId,
        Hero::getLevel,
        Hero::getName,
        Hero::getOtherName,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_LEVEL,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

		verify(this.heroService).findRandomHero();
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldNotAddInvalidItem() {
		var hero = new Hero();
		hero.setName(null);
		hero.setOtherName(DEFAULT_OTHER_NAME);
		hero.setPicture(DEFAULT_PICTURE);
		hero.setPowers(DEFAULT_POWERS);
		hero.setLevel(0);

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/heroes")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldNotAddNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/heroes")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldNotFullyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.put("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldNotFullyUpdateInvalidItem() {
		var hero = createFullyUpdatedHero();
		hero.setName(null);
		hero.setOtherName(UPDATED_OTHER_NAME);
		hero.setPicture(UPDATED_PICTURE);
		hero.setPowers(UPDATED_PICTURE);
		hero.setLevel(0);

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/heroes/{id}", hero.getId())
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldNotPartiallyUpdateInvalidItem() {
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getName() == null) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getPicture().equals(UPDATED_PICTURE) &&
				h.getPowers().equals(UPDATED_POWERS) &&
				(h.getLevel() == 0);

		when(this.heroService.partialUpdateHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().failure(new ConstraintViolationException(Set.of())));

		var hero = createPartiallyUpdatedHero();
		hero.setName(null);
		hero.setOtherName(UPDATED_OTHER_NAME);
		hero.setLevel(0);

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verify(this.heroService).partialUpdateHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldNotPartiallyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.patch("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.heroService);
	}

	@Test
	public void shouldGetItems() {
		when(this.heroService.findAllHeroes())
			.thenReturn(Uni.createFrom().item(List.of(createDefaultHero())));

		var heroes = get("/api/heroes")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Hero.class);

    assertThat(heroes)
      .singleElement()
      .extracting(
        Hero::getId,
        Hero::getLevel,
        Hero::getName,
        Hero::getOtherName,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_LEVEL,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

		verify(this.heroService).findAllHeroes();
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldGetEmptyItems() {
		when(this.heroService.findAllHeroes())
			.thenReturn(Uni.createFrom().item(List.of()));

		get("/api/heroes")
			.then()
				.statusCode(OK.getStatusCode())
				.body("$.size()", is(0));

		verify(this.heroService).findAllHeroes();
		verifyNoMoreInteractions(this.heroService);
	}

  @Test
  public void shouldGetItemsWithNameFilter() {
    when(this.heroService.findAllHeroesHavingName(eq("name")))
      .thenReturn(Uni.createFrom().item(List.of(createDefaultHero())));

    var heroes = given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/heroes")
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Hero.class);

    assertThat(heroes)
      .singleElement()
      .extracting(
        Hero::getId,
        Hero::getLevel,
        Hero::getName,
        Hero::getOtherName,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_LEVEL,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        DEFAULT_PICTURE,
        DEFAULT_POWERS
      );

    verify(this.heroService).findAllHeroesHavingName(eq("name"));
    verifyNoMoreInteractions(this.heroService);
  }

  @Test
  public void shouldGetEmptyItemsWithNameFilter() {
    when(this.heroService.findAllHeroesHavingName(eq("name")))
      .thenReturn(Uni.createFrom().item(List.of()));

    given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/heroes")
      .then()
        .statusCode(OK.getStatusCode())
        .body("$.size()", is(0));

    verify(this.heroService).findAllHeroesHavingName(eq("name"));
    verifyNoMoreInteractions(this.heroService);
  }

	@Test
	public void shouldGetNullItems() {
		when(this.heroService.findAllHeroes())
			.thenReturn(Uni.createFrom().item(List.of()));

		get("/api/heroes")
			.then()
			.statusCode(OK.getStatusCode())
			.body("$.size()", is(0));

		verify(this.heroService).findAllHeroes();
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldAddAnItem() {
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == null) &&
				h.getName().equals(DEFAULT_NAME) &&
				h.getOtherName().equals(DEFAULT_OTHER_NAME) &&
				h.getPicture().equals(DEFAULT_PICTURE) &&
				h.getPowers().equals(DEFAULT_POWERS) &&
				(h.getLevel() == DEFAULT_LEVEL);

		when(this.heroService.persistHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().item(createDefaultHero()));

		var hero = new Hero();
		hero.setName(DEFAULT_NAME);
		hero.setOtherName(DEFAULT_OTHER_NAME);
		hero.setPicture(DEFAULT_PICTURE);
		hero.setPowers(DEFAULT_POWERS);
		hero.setLevel(DEFAULT_LEVEL);

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/heroes")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, containsString("/api/heroes/" + DEFAULT_ID));

		verify(this.heroService).persistHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldNotFullyUpdateNotFoundItem() {
		var hero = createFullyUpdatedHero();
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				h.getName().equals(UPDATED_NAME) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getPicture().equals(UPDATED_PICTURE) &&
				h.getPowers().equals(UPDATED_POWERS) &&
				(h.getLevel() == UPDATED_LEVEL);

		when(this.heroService.replaceHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().nullItem());

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/heroes/{id}", hero.getId())
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.heroService).replaceHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldFullyUpdateAnItem() {
		var hero = createFullyUpdatedHero();
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				h.getName().equals(UPDATED_NAME) &&
				h.getOtherName().equals(UPDATED_OTHER_NAME) &&
				h.getPicture().equals(UPDATED_PICTURE) &&
				h.getPowers().equals(UPDATED_POWERS) &&
				(h.getLevel() == UPDATED_LEVEL);

		when(this.heroService.replaceHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().item(hero));

		given()
			.when()
				.body(hero)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/heroes/{id}", hero.getId())
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.heroService).replaceHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldNotPartiallyUpdateNotFoundItem() {
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getName() == null) &&
				(h.getOtherName() == null) &&
				h.getPicture().equals(UPDATED_PICTURE) &&
				h.getPowers().equals(UPDATED_POWERS) &&
				(h.getLevel() == null);

		var partialHero = new Hero();
		partialHero.setPowers(UPDATED_POWERS);
		partialHero.setPicture(UPDATED_PICTURE);

		when(this.heroService.partialUpdateHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().nullItem());

		given()
			.when()
				.body(partialHero)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.heroService).partialUpdateHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldPartiallyUpdateAnItem() {
		ArgumentMatcher<Hero> heroMatcher = h ->
			(h.getId() == DEFAULT_ID) &&
				(h.getName() == null) &&
				(h.getOtherName() == null) &&
				h.getPicture().equals(UPDATED_PICTURE) &&
				h.getPowers().equals(UPDATED_POWERS) &&
				(h.getLevel() == null);

		var partialHero = new Hero();
		partialHero.setPowers(UPDATED_POWERS);
		partialHero.setPicture(UPDATED_PICTURE);

		when(this.heroService.partialUpdateHero(argThat(heroMatcher)))
			.thenReturn(Uni.createFrom().item(createPartiallyUpdatedHero()));

		var hero = given()
			.when()
				.body(partialHero)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Hero.class);

    assertThat(hero)
      .isNotNull()
      .extracting(
        Hero::getId,
        Hero::getLevel,
        Hero::getName,
        Hero::getOtherName,
        Hero::getPicture,
        Hero::getPowers
      )
      .containsExactly(
        DEFAULT_ID,
        DEFAULT_LEVEL,
        DEFAULT_NAME,
        DEFAULT_OTHER_NAME,
        UPDATED_PICTURE,
        UPDATED_POWERS
      );

		verify(this.heroService).partialUpdateHero(argThat(heroMatcher));
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldDeleteHero() {
		when(this.heroService.deleteHero(eq(DEFAULT_ID))).thenReturn(Uni.createFrom().voidItem());

		delete("/api/heroes/{id}", DEFAULT_ID)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.heroService).deleteHero(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.heroService);
	}

  @Test
  public void shouldReplaceAllHeroes() {
    var heroes = List.of(createDefaultHero(), createFullyUpdatedHero());
    heroes.forEach(h -> h.setId(null));

    ArgumentMatcher<List<Hero>> heroesMatcher = h ->
      (h.size() == 2) &&
			(h.get(0).getId() == null) &&
			h.get(0).getName().equals(DEFAULT_NAME) &&
			h.get(0).getOtherName().equals(DEFAULT_OTHER_NAME) &&
			h.get(0).getPicture().equals(DEFAULT_PICTURE) &&
			h.get(0).getPowers().equals(DEFAULT_POWERS) &&
			(h.get(0).getLevel() == DEFAULT_LEVEL) &&
      (h.get(1).getId() == null) &&
			h.get(1).getName().equals(UPDATED_NAME) &&
			h.get(1).getOtherName().equals(UPDATED_OTHER_NAME) &&
			h.get(1).getPicture().equals(UPDATED_PICTURE) &&
			h.get(1).getPowers().equals(UPDATED_POWERS) &&
			(h.get(1).getLevel() == UPDATED_LEVEL);

    when(this.heroService.replaceAllHeroes(argThat(heroesMatcher)))
      .thenReturn(Uni.createFrom().voidItem());

		given()
			.when()
				.body(heroes)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/heroes")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, Matchers.endsWith("/api/heroes"));

    verify(this.heroService).replaceAllHeroes(argThat(heroesMatcher));
    verifyNoMoreInteractions(this.heroService);
  }

	@Test
	public void shouldDeleteAllHeros() {
		when(this.heroService.deleteAllHeroes()).thenReturn(Uni.createFrom().voidItem());

		delete("/api/heroes")
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.heroService).deleteAllHeroes();
		verifyNoMoreInteractions(this.heroService);
	}

	@Test
	public void shouldPingOpenAPI() {
		get("/q/openapi")
			.then().statusCode(OK.getStatusCode());
	}

	private static Hero createDefaultHero() {
		var hero = new Hero();
		hero.setId(DEFAULT_ID);
		hero.setName(DEFAULT_NAME);
		hero.setOtherName(DEFAULT_OTHER_NAME);
		hero.setPicture(DEFAULT_PICTURE);
		hero.setPowers(DEFAULT_POWERS);
		hero.setLevel(DEFAULT_LEVEL);

		return hero;
	}

	public static Hero createFullyUpdatedHero() {
		var hero = createDefaultHero();
		hero.setName(UPDATED_NAME);
		hero.setOtherName(UPDATED_OTHER_NAME);
		hero.setPicture(UPDATED_PICTURE);
		hero.setPowers(UPDATED_POWERS);
		hero.setLevel(UPDATED_LEVEL);

		return hero;
	}

	public static Hero createPartiallyUpdatedHero() {
		var hero = createDefaultHero();
		hero.setPicture(UPDATED_PICTURE);
		hero.setPowers(UPDATED_POWERS);

		return hero;
	}
}
