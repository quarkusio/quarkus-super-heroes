package io.quarkus.sample.superheroes.villain.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.service.VillainService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VillainResourceTests {
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
	VillainService villainService;

	@Test
	public void helloEndpoint() {
		get("/api/villains/hello")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(TEXT)
				.body(is("Hello Villain Resource"));

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldNotGetUnknownVillain() {
		when(this.villainService.findVillainById(eq(DEFAULT_ID)))
			.thenReturn(Optional.empty());

		get("/api/villains/{id}", DEFAULT_ID)
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.villainService).findVillainById(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldGetRandomVillainNotFound() {
		when(this.villainService.findRandomVillain())
			.thenReturn(Optional.empty());

		get("/api/villains/random")
			.then().statusCode(NOT_FOUND.getStatusCode());

		verify(this.villainService).findRandomVillain();
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldGetRandomVillainFound() {
		when(this.villainService.findRandomVillain())
			.thenReturn(Optional.of(createDefaultVillian()));

		var villain = get("/api/villains/random")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Villain.class);

    var defaultVillain = new Villain();
    defaultVillain.id = DEFAULT_ID;
    defaultVillain.name = DEFAULT_NAME;
    defaultVillain.otherName = DEFAULT_OTHER_NAME;
    defaultVillain.picture = DEFAULT_PICTURE;
    defaultVillain.powers = DEFAULT_POWERS;
    defaultVillain.level = DEFAULT_LEVEL;

    assertThat(villain)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultVillain);

		verify(this.villainService).findRandomVillain();
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldNotAddInvalidItem() {
		var villain = new Villain();
		villain.name = null;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = 0;

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/villains")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldNotAddNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.post("/api/villains")
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldNotFullyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.put("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldNotFullyUpdateInvalidItem() {
		var villain = createFullyUpdatedVillain();
		villain.name = null;
		villain.otherName = UPDATED_OTHER_NAME;
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_PICTURE;
		villain.level = 0;

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/villains/{id}", villain.id)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldNotPartiallyUpdateInvalidItem() {
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null) &&
				v.otherName.equals(UPDATED_OTHER_NAME) &&
				v.picture.equals(UPDATED_PICTURE) &&
				v.powers.equals(UPDATED_POWERS) &&
				(v.level == 0);

		when(this.villainService.partialUpdateVillain(argThat(villainMatcher)))
			.thenThrow(new ConstraintViolationException(Set.of()));

		var villain = createPartiallyUpdatedVillain();
		villain.name = null;
		villain.otherName = UPDATED_OTHER_NAME;
		villain.level = 0;

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verify(this.villainService).partialUpdateVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldNotPartiallyUpdateNullItem() {
		given()
			.when()
				.contentType(JSON)
				.accept(JSON)
				.body("")
				.patch("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(BAD_REQUEST.getStatusCode());

		verifyNoInteractions(this.villainService);
	}

	@Test
	public void shouldGetItems() {
		when(this.villainService.findAllVillains())
			.thenReturn(List.of(createDefaultVillian()));

    var defaultVillain = new Villain();
    defaultVillain.id = DEFAULT_ID;
    defaultVillain.name = DEFAULT_NAME;
    defaultVillain.otherName = DEFAULT_OTHER_NAME;
    defaultVillain.picture = DEFAULT_PICTURE;
    defaultVillain.powers = DEFAULT_POWERS;
    defaultVillain.level = DEFAULT_LEVEL;

		var villains = get("/api/villains")
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Villain.class);

    assertThat(villains)
      .singleElement()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultVillain);

		verify(this.villainService).findAllVillains();
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldGetEmptyItems() {
		when(this.villainService.findAllVillains())
			.thenReturn(List.of());

		get("/api/villains")
			.then()
				.statusCode(OK.getStatusCode())
				.body("$.size()", is(0));

		verify(this.villainService).findAllVillains();
		verifyNoMoreInteractions(this.villainService);
	}

  @Test
  public void shouldGetItemsWithNameFilter() {
    when(this.villainService.findAllVillainsHavingName(eq("name")))
      .thenReturn(List.of(createDefaultVillian()));

    var defaultVillain = new Villain();
    defaultVillain.id = DEFAULT_ID;
    defaultVillain.name = DEFAULT_NAME;
    defaultVillain.otherName = DEFAULT_OTHER_NAME;
    defaultVillain.picture = DEFAULT_PICTURE;
    defaultVillain.powers = DEFAULT_POWERS;
    defaultVillain.level = DEFAULT_LEVEL;

    var villains = given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/villains")
      .then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().body()
        .jsonPath().getList(".", Villain.class);

    assertThat(villains)
      .singleElement()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultVillain);

    verify(this.villainService).findAllVillainsHavingName(eq("name"));
    verifyNoMoreInteractions(this.villainService);
  }

  @Test
  public void shouldGetEmptyItemsWithNameFilter() {
    when(this.villainService.findAllVillainsHavingName(eq("name")))
      .thenReturn(List.of());

    given()
      .when()
        .queryParam("name_filter", "name")
        .get("/api/villains")
      .then()
        .statusCode(OK.getStatusCode())
        .body("$.size()", is(0));

    verify(this.villainService).findAllVillainsHavingName(eq("name"));
    verifyNoMoreInteractions(this.villainService);
  }

	@Test
	public void shouldGetNullItems() {
		when(this.villainService.findAllVillains())
			.thenReturn(List.of());

		get("/api/villains")
			.then()
				.statusCode(OK.getStatusCode())
				.body("$.size()", is(0));

		verify(this.villainService).findAllVillains();
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldAddAnItem() {
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == null) &&
			v.name.equals(DEFAULT_NAME) &&
			v.otherName.equals(DEFAULT_OTHER_NAME) &&
			v.picture.equals(DEFAULT_PICTURE) &&
			v.powers.equals(DEFAULT_POWERS) &&
			(v.level == DEFAULT_LEVEL);

		when(this.villainService.persistVillain(argThat(villainMatcher)))
			.thenReturn(createDefaultVillian());

		var villain = new Villain();
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = DEFAULT_LEVEL;

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.post("/api/villains")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, containsString("/api/villains/" + DEFAULT_ID));

		verify(this.villainService).persistVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldNotFullyUpdateNotFoundItem() {
		var villain = createFullyUpdatedVillain();
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == DEFAULT_ID) &&
				v.name.equals(UPDATED_NAME) &&
				v.otherName.equals(UPDATED_OTHER_NAME) &&
				v.picture.equals(UPDATED_PICTURE) &&
				v.powers.equals(UPDATED_POWERS) &&
				(v.level == UPDATED_LEVEL);

		when(this.villainService.replaceVillain(argThat(villainMatcher)))
			.thenReturn(Optional.empty());

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/villains/{id}", villain.id)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.villainService).replaceVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldFullyUpdateAnItem() {
		var villain = createFullyUpdatedVillain();
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == DEFAULT_ID) &&
				v.name.equals(UPDATED_NAME) &&
				v.otherName.equals(UPDATED_OTHER_NAME) &&
				v.picture.equals(UPDATED_PICTURE) &&
				v.powers.equals(UPDATED_POWERS) &&
				(v.level == UPDATED_LEVEL);

		when(this.villainService.replaceVillain(argThat(villainMatcher)))
			.thenReturn(Optional.of(villain));

		given()
			.when()
				.body(villain)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/villains/{id}", villain.id)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.villainService).replaceVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldNotPartiallyUpdateNotFoundItem() {
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null) &&
				(v.otherName == null) &&
				v.picture.equals(UPDATED_PICTURE) &&
				v.powers.equals(UPDATED_POWERS) &&
				(v.level == null);

		var partialVillain = new Villain();
		partialVillain.powers = UPDATED_POWERS;
		partialVillain.picture = UPDATED_PICTURE;

		when(this.villainService.partialUpdateVillain(argThat(villainMatcher)))
			.thenReturn(Optional.empty());

		given()
			.when()
				.body(partialVillain)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(NOT_FOUND.getStatusCode())
				.body(blankOrNullString());

		verify(this.villainService).partialUpdateVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldPartiallyUpdateAnItem() {
		ArgumentMatcher<Villain> villainMatcher = v ->
			(v.id == DEFAULT_ID) &&
				(v.name == null) &&
				(v.otherName == null) &&
				v.picture.equals(UPDATED_PICTURE) &&
				v.powers.equals(UPDATED_POWERS) &&
				(v.level == null);

		var partialVillain = new Villain();
		partialVillain.powers = UPDATED_POWERS;
		partialVillain.picture = UPDATED_PICTURE;

    var defaultVillain = new Villain();
    defaultVillain.id = DEFAULT_ID;
    defaultVillain.name = DEFAULT_NAME;
    defaultVillain.otherName = DEFAULT_OTHER_NAME;
    defaultVillain.picture = partialVillain.picture;
    defaultVillain.powers = partialVillain.powers;
    defaultVillain.level = DEFAULT_LEVEL;

		when(this.villainService.partialUpdateVillain(argThat(villainMatcher)))
			.thenReturn(Optional.of(createPartiallyUpdatedVillain()));

		var villain = given()
			.when()
				.body(partialVillain)
				.contentType(JSON)
				.accept(JSON)
				.patch("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(OK.getStatusCode())
				.contentType(JSON)
        .extract().as(Villain.class);

    assertThat(villain)
      .isNotNull()
      .usingRecursiveComparison()
      .ignoringFieldsMatchingRegexes(".*_hibernate_.*")
      .isEqualTo(defaultVillain);

		verify(this.villainService).partialUpdateVillain(argThat(villainMatcher));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldDeleteVillain() {
		doNothing()
			.when(this.villainService)
			.deleteVillain(eq(DEFAULT_ID));

		delete("/api/villains/{id}", DEFAULT_ID)
			.then()
				.statusCode(NO_CONTENT.getStatusCode())
				.body(blankOrNullString());

		verify(this.villainService).deleteVillain(eq(DEFAULT_ID));
		verifyNoMoreInteractions(this.villainService);
	}

	@Test
	public void shouldDeleteAllVillains() {
		doNothing()
			.when(this.villainService)
			.deleteAllVillains();

		delete("/api/villains")
			.then()
			.statusCode(NO_CONTENT.getStatusCode())
			.body(blankOrNullString());

		verify(this.villainService).deleteAllVillains();
		verifyNoMoreInteractions(this.villainService);
	}

  @Test
  public void shouldReplaceAllVillains() {
    var villains = List.of(createDefaultVillian(), createFullyUpdatedVillain());
    villains.forEach(v -> v.id = null);

    ArgumentMatcher<List<Villain>> villainsMatcher = v ->
      (v.size() == 2) &&
			(v.get(0).id == null) &&
			v.get(0).name.equals(DEFAULT_NAME) &&
			v.get(0).otherName.equals(DEFAULT_OTHER_NAME) &&
			v.get(0).picture.equals(DEFAULT_PICTURE) &&
			v.get(0).powers.equals(DEFAULT_POWERS) &&
			(v.get(0).level == DEFAULT_LEVEL) &&
      (v.get(1).id == null) &&
			v.get(1).name.equals(UPDATED_NAME) &&
			v.get(1).otherName.equals(UPDATED_OTHER_NAME) &&
			v.get(1).picture.equals(UPDATED_PICTURE) &&
			v.get(1).powers.equals(UPDATED_POWERS) &&
			(v.get(1).level == UPDATED_LEVEL);

    doNothing()
      .when(this.villainService)
      .replaceAllVillains(argThat(villainsMatcher));

		given()
			.when()
				.body(villains)
				.contentType(JSON)
				.accept(JSON)
				.put("/api/villains")
			.then()
				.statusCode(CREATED.getStatusCode())
				.header(HttpHeaders.LOCATION, Matchers.endsWith("/api/villains"));

    verify(this.villainService).replaceAllVillains(argThat(villainsMatcher));
    verifyNoMoreInteractions(this.villainService);
  }

	@Test
	public void shouldPingOpenAPI() {
		given()
			.when()
				.accept(JSON)
				.get("/q/openapi")
			.then()
				.statusCode(OK.getStatusCode());
	}

	private static Villain createDefaultVillian() {
		var villain = new Villain();
		villain.id = DEFAULT_ID;
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		villain.level = DEFAULT_LEVEL;

		return villain;
	}

	public static Villain createFullyUpdatedVillain() {
		var villain = createDefaultVillian();
		villain.name = UPDATED_NAME;
		villain.otherName = UPDATED_OTHER_NAME;
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_POWERS;
		villain.level = UPDATED_LEVEL;

		return villain;
	}

	public static Villain createPartiallyUpdatedVillain() {
		var villain = createDefaultVillian();
		villain.picture = UPDATED_PICTURE;
		villain.powers = UPDATED_POWERS;

		return villain;
	}
}
