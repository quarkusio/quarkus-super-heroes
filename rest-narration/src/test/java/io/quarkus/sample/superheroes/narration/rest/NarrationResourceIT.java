package io.quarkus.sample.superheroes.narration.rest;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class NarrationResourceIT {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final String NARRATION = """
                                          High above a bustling city, a symbol of hope and justice soared through the sky, while chaos reigned below, with malevolent laughter echoing through the streets.
                                          With unwavering determination, the figure swiftly descended, effortlessly evading explosive attacks, closing the gap, and delivering a decisive blow that silenced the wicked laughter.
                                          
                                          In the end, the battle concluded with a clear victory for the forces of good, as their commitment to peace triumphed over the chaos and villainy that had threatened the city.
                                          The people knew that their protector had once again ensured their safety.
                                          """;
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

  @Test
  void shouldPingOpenAPI() {
    given()
      .accept(JSON)
      .get("/q/openapi").then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON);
  }

  @Test
  public void helloEndpoint() {
    get("/api/narration/hello").then()
      .statusCode(OK.getStatusCode())
      .contentType(TEXT)
      .body(is("Hello Narration Resource"));
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
  }

  @Test
  public void invalidFightToNarrate() {
    given()
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(400);
  }
}
