package io.quarkus.sample.superheroes.narration.rest;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.service.NarrationService;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.restassured.RestAssured;

@ConnectWireMock
abstract class NarrationResourceIT {
  private static final String HERO_NAME = "Super Baguette";
  private static final int HERO_LEVEL = 42;
  private static final String HERO_POWERS = "Eats baguette in less than a second";
  private static final String HERO_TEAM_NAME = "heroes";
  private static final String VILLAIN_NAME = "Super Chocolatine";
  private static final int VILLAIN_LEVEL = 43;
  private static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  private static final String VILLAIN_TEAM_NAME = "villains";
  private static final String EXPECTED_NARRATION = "In the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\n\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\n\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\n\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity.";

  private static final String DEFAULT_LOCATION_NAME = "Gotham City";
  private static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  private static final Fight FIGHT = new Fight(VILLAIN_NAME, VILLAIN_LEVEL, VILLAIN_POWERS, HERO_NAME, HERO_LEVEL, HERO_POWERS, VILLAIN_TEAM_NAME, HERO_TEAM_NAME, new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION));
  protected static final String REQUEST_JSON = """
    {
      "model": "gpt-3.5-turbo",
      "messages": [
        {
          "role": "system",
          "content": "You are a marvel comics writer, expert in all sorts of super heroes and super villains."
        },
        {
          "role": "user",
          "content": "Narrate the fight between a super hero and a super villain.\\n\\nDuring the narration, don't repeat \\"super hero\\" or \\"super villain\\".\\n\\nWrite 4 paragraphs maximum. Be creative.\\n\\nThe narration must be:\\n- G rated\\n- Workplace/family safe\\n- No sexism, racism, or other bias/bigotry\\n\\nHere is the data you will use for the winner:\\n\\n+++++\\nName: %s\\nPowers: %s\\nLevel: %d\\n+++++\\n\\nHere is the data you will use for the loser:\\n\\n+++++\\nName: %s\\nPowers: %s\\nLevel: %d\\n+++++\\n\\nHere is the data you will use for the fight:\\n\\n+++++\\n%s who is a %s has won the fight against %s who is a %s.\\n\\nThe fight took place in %s, which can be described as %s.\\n+++++\\n"
        }
      ],
      "temperature": 0.7,
      "top_p": 0.5,
      "presence_penalty": 0,
      "frequency_penalty": 0
    }
    """.formatted(
    FIGHT.winnerName(),
    FIGHT.winnerPowers(),
    FIGHT.winnerLevel(),
    FIGHT.loserName(),
    FIGHT.loserPowers(),
    FIGHT.loserLevel(),
    FIGHT.winnerName(),
    FIGHT.winnerTeam(),
    FIGHT.loserName(),
    FIGHT.loserTeam(),
    FIGHT.location().name(),
    FIGHT.location().description()
  );

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
  void invalidFightToNarrate() {
    given()
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(400);
  }
}
