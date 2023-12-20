package io.quarkus.sample.superheroes.narration.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.service.NarrationProcessor;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.restassured.RestAssured;

@QuarkusTest
class NarrationResourceTest {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final String NARRATION = "Lorem ipsum dolor sit amet";
  private static final String DEFAULT_LOCATION_NAME = "Gotham City";
  private static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  private static final Fight FIGHT = new Fight(
    VILLAIN_NAME,
    VILLAIN_LEVEL,
    VILLAIN_POWERS,
    HERO_NAME,
    HERO_LEVEL,
    HERO_POWERS,
    VILLAIN_TEAM_NAME,
    HERO_TEAM_NAME,
    new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION)
  );

  @InjectMock
  NarrationProcessor narrationProcessor;

  @BeforeAll
	static void beforeAll() {
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

  @BeforeEach
  public void setup() {
    when(this.narrationProcessor.narrate(FIGHT))
      .thenReturn(NARRATION);
  }

  @Test
  void shouldPingOpenAPI() {
    given()
      .accept(JSON)
      .when().get("/q/openapi").then()
      .statusCode(OK.getStatusCode())
      .contentType(JSON);

    verifyNoInteractions(this.narrationProcessor);
  }

  @Test
  void helloEndpoint() {
    get("/api/narration/hello").then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Narration Resource"));

    verifyNoInteractions(this.narrationProcessor);
  }

  @Test
  void shouldNarrateAFight() {
    given()
      .body(FIGHT)
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(NARRATION));

    verify(this.narrationProcessor).narrate(FIGHT);
    verifyNoMoreInteractions(this.narrationProcessor);
  }

  @Test
  void invalidFightToNarrate() {
    given()
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(400);

    verifyNoInteractions(this.narrationProcessor);
  }
}
