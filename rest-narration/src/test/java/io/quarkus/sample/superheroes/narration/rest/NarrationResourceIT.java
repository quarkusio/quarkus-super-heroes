package io.quarkus.sample.superheroes.narration.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.*;
import static jakarta.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.rest.NarrationResourceIT.WiremockOpenAITestProfile;
import io.quarkus.sample.superheroes.narration.service.ImageGenerationService;
import io.quarkus.sample.superheroes.narration.service.NarrationService;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
@TestProfile(WiremockOpenAITestProfile.class)
@ConnectWireMock
class NarrationResourceIT {
  private static final String EXPECTED_IMAGE_URL = "https://somewhere.com/someImage.png";
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

  private static final String NARRATION_REQUEST_JSON = """
    {
      "model": "gpt-4o-mini",
      "messages": [
        {
          "role": "system",
          "content": "You are a marvel comics writer, expert in all sorts of super heroes and super villains."
        },
        {
          "role": "user",
          "content": "Narrate the matchup between a super hero and a super villain.\\n\\nDuring the narration, don't repeat \\"super hero\\" or \\"super villain\\".\\n\\nWrite 4 paragraphs maximum. Be creative and overly funny.\\n\\nThe narration must be:\\n- G rated\\n- Workplace/family safe\\n- No sexism, racism, or other bias/bigotry\\n- No overt violence. It needs to pass your own safety system and standards. Don't reference violence.\\n\\nHere is the data you will use for the winner:\\n\\n+++++\\nName: %s\\nPowers: %s\\nLevel: %d\\n+++++\\n\\nHere is the data you will use for the loser:\\n\\n+++++\\nName: %s\\nPowers: %s\\nLevel: %d\\n+++++\\n\\nHere is the data you will use for the fight:\\n\\n+++++\\n%s who is a %s has won the fight against %s who is a %s.\\n\\nThe fight took place in %s, which can be described as %s.\\n+++++\\n"
        }
      ],
      "temperature": 1.0,
      "top_p": 0.5,
      "presence_penalty": 0.0,
      "frequency_penalty": 0.0
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

  private static final String IMAGE_REQUEST_JSON = """
    {
      "model": "gpt-image-2",
      "prompt": "%s\\nIn the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\\n\\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\\n\\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\\n\\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity.\\nYou must answer strictly in the following JSON format: {\\n\\"url\\": (type: java.net.URI),\\n\\"base64Data\\": (type: string),\\n\\"mimeType\\": (type: string),\\n\\"revisedPrompt\\": (type: string)\\n}",
      "n": 1,
      "size": "1024x1024",
      "quality": "low",
      "moderation": "low"
    }
    """.formatted(ImageGenerationService.SYSTEM_MESSAGE.replaceAll("\n", "\\\\n"));

  WireMock wireMock;

  @BeforeAll
  static void beforeAll() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @BeforeEach
  public void beforeEach() {
    this.wireMock.resetToDefaultMappings();
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

    this.wireMock.verifyThat(
      1,
      postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
    );
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
      .extracting(FightImage::imageUrl)
      .isEqualTo(EXPECTED_IMAGE_URL);

    this.wireMock.verifyThat(
      1,
      postRequestedFor(urlPathEqualTo("/v1/images/generations"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(IMAGE_REQUEST_JSON, true, false))
    );
  }

  @Test
  void shouldGetAFallbackOnError() {
    this.wireMock.register(
      post(urlPathEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
        .willReturn(serverError())
    );

    given()
      .body(FIGHT)
      .contentType(JSON)
      .accept(TEXT)
      .when().post("/api/narration").then()
        .statusCode(OK.getStatusCode())
        .contentType(TEXT)
        .body(is(NarrationService.FALLBACK_NARRATION));

    this.wireMock.verifyThat(
      3,
      postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
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

  public static class WiremockOpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      var hostname = Boolean.getBoolean("quarkus.container-image.build") ? "host.docker.internal" : "localhost";

      return Map.of(
        "quarkus.langchain4j.openai.enable-integration", "true",
        "quarkus.langchain4j.openai.log-requests", "true",
        "quarkus.langchain4j.openai.log-responses", "true",
        "quarkus.langchain4j.openai.base-url", "http://%s:${%s}/v1/".formatted(hostname, WireMockConfigKey.PORT),
        "quarkus.langchain4j.openai.max-retries", "2",
        "quarkus.langchain4j.openai.timeout", "3s"
      );
    }
  }
}
