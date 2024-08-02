package io.quarkus.sample.superheroes.narration.rest;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.service.NarrationService;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.restassured.RestAssured;

@ConnectWireMock
abstract class NarrationResourceIT {
  protected static final String EXPECTED_IMAGE_NARRATION = "In a city reminiscent of a dark, gritty metropolitan at night, two characters are caught in a confrontation of epic proportions. One figure, a heroic roguish character, renowned for his agile precision and disbelief in mystic powers, is engaged in a standoff against a menacing adversary, outfitted in futuristic armor and armed with a modest energy weapon. The hero, quick on his feet, evades his opponent's inadequate shots while retorting with his more formidable blaster causing loud reverberations throughout the urban landscape. Despite being outgunned, the villain continues the fight, showing a relentless spirit. With each passing second, the hero's superior skill and command over his weapon become increasingly visible. His counter-attacks are perfectly timed and lethal, overwhelming the adversary struggling to match him. Displaying a growing confidence, the hero's movements transition to being more seamless. A final, pinpoint shot from the hero incapacitates the villain, signaling a decisive victory for the hero, much to the city's relief. The hero's unwavering resilience standing victorious brings hope and strength to a city under duress.";
  protected static final String EXPECTED_IMAGE_URL = "https://somewhere.com/someImage.png";
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  protected static final String EXPECTED_NARRATION = "In the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\n\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\n\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\n\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity.";

  private static final String DEFAULT_LOCATION_NAME = "Gotham City";
  private static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  protected static final Fight FIGHT = new Fight(VILLAIN_NAME, VILLAIN_LEVEL, VILLAIN_POWERS, HERO_NAME, HERO_LEVEL, HERO_POWERS, VILLAIN_TEAM_NAME, HERO_TEAM_NAME, new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION));

  protected static final String IMAGE_REQUEST_JSON = """
    {
      "model": "dall-e-3",
      "prompt": "In the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\\n\\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\\n\\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\\n\\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity.",
      "n": 1,
      "size": "1024x1024",
      "quality": "standard",
      "style": "vivid",
      "response_format": "url"
    }
    """;

  WireMock wireMock;

  @BeforeAll
  static void beforeAll() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @BeforeEach
  public void beforeEach() {
    this.wireMock.resetToDefaultMappings();
  }

  static boolean azureOpenAiEnabled() {
    return "azure-openai".equals(System.getenv("QUARKUS_TEST_INTEGRATION_TEST_PROFILE")) ||
      "azure-openai".equals(System.getProperty("quarkus.test.integration-test-profile"));
  }

  @Test
  void shouldPingOpenAPI() {
    given()
      .accept(JSON)
      .get("/q/openapi").then()
        .statusCode(OK.getStatusCode())
      .contentType(JSON);
  }

  @Test
  void helloEndpoint() {
    RestAssured.get("/api/narration/hello").then()
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
        .body(is(EXPECTED_NARRATION));
  }

  @Test
  void shouldGetAFallbackOnError() {
    given()
      .body(FIGHT)
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(NarrationService.FALLBACK_NARRATION));
  }

  @Test
  void shouldGenerateAnImageFromNarration() {
    var generatedImage = given()
      .body(EXPECTED_NARRATION)
      .contentType(TEXT)
      .accept(JSON)
      .when().post("/api/narration/image").then()
        .statusCode(OK.getStatusCode())
        .contentType(JSON)
        .extract().as(FightImage.class);

    assertThat(generatedImage)
      .isNotNull()
      .extracting(
        FightImage::imageNarration,
        FightImage::imageUrl
      )
      .containsExactly(
        EXPECTED_IMAGE_NARRATION,
        EXPECTED_IMAGE_URL
      );
  }

  @Test
  void invalidFightToNarrate() {
    given()
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }

  @Test
  void invalidNarrationToFetchImage() {
    given()
      .contentType(TEXT)
      .accept(JSON)
      .when()
      .post("/api/narration/image").then()
        .statusCode(BAD_REQUEST.getStatusCode());
  }
}
