package io.quarkus.sample.superheroes.narration.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import io.quarkus.sample.superheroes.narration.rest.AzureOpenAiNarrationResourceIT.WiremockAzureOpenAITestProfile;

import io.quarkiverse.wiremock.devservice.WireMockConfigKey;

@QuarkusIntegrationTest
@TestProfile(WiremockAzureOpenAITestProfile.class)
@EnabledIf(value = "azureOpenAiEnabled", disabledReason = "Azure OpenAI profile is not enabled")
class AzureOpenAiNarrationResourceIT extends NarrationResourceIT {
	private static final String NARRATION_REQUEST_JSON = """
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

	@Test
  @Override
	void shouldNarrateAFight() {
		super.shouldNarrateAFight();

		this.wireMock.verifyThat(
      1,
      postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
	      .withQueryParam("api-version", matching(".+"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader("api-key", equalTo("change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
    );
	}

  @Test
  @Override
  void shouldGenerateAnImageFromNarration() {
    super.shouldGenerateAnImageFromNarration();

    this.wireMock.verifyThat(
      1,
      postRequestedFor(urlPathEqualTo("/v1/images/generations"))
        .withQueryParam("api-version", matching(".+"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader("api-key", equalTo("change-me"))
        .withRequestBody(equalToJson(IMAGE_REQUEST_JSON, true, false))
    );
  }

  @Test
	@Override
	void shouldGetAFallbackOnError() {
		this.wireMock.register(
      post(urlPathEqualTo("/v1/chat/completions"))
	      .withQueryParam("api-version", matching(".+"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader("api-key", equalTo("change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
        .willReturn(serverError())
    );

		super.shouldGetAFallbackOnError();

		this.wireMock.verifyThat(
      2,
      postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
	      .withQueryParam("api-version", matching(".+"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader("api-key", equalTo("change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
    );
	}

	public static class WiremockAzureOpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
	    var hostname = Boolean.getBoolean("quarkus.container-image.build") ? "host.docker.internal" : "localhost";

      var vals = new HashMap<>(Map.of(
				"quarkus.langchain4j.azure-openai.enable-integration", "true",
        "quarkus.langchain4j.azure-openai.log-requests", "true",
        "quarkus.langchain4j.azure-openai.log-responses", "true",
        "quarkus.langchain4j.azure-openai.endpoint", "http://%s:${%s}/v1/".formatted(hostname, WireMockConfigKey.PORT),
        "quarkus.langchain4j.azure-openai.max-retries", "2",
        "quarkus.langchain4j.azure-openai.timeout", "3s"
      ));

      vals.put("quarkus.langchain4j.azure-openai.dalle3.enable-integration", vals.get("quarkus.langchain4j.azure-openai.enable-integration"));
      vals.put("quarkus.langchain4j.azure-openai.dalle3.log-requests", vals.get("quarkus.langchain4j.azure-openai.log-requests"));
      vals.put("quarkus.langchain4j.azure-openai.dalle3.log-responses", vals.get("quarkus.langchain4j.azure-openai.log-responses"));
      vals.put("quarkus.langchain4j.azure-openai.dalle3.endpoint", vals.get("quarkus.langchain4j.azure-openai.endpoint"));
      vals.put("quarkus.langchain4j.azure-openai.dalle3.max-retries", vals.get("quarkus.langchain4j.azure-openai.max-retries"));
      vals.put("quarkus.langchain4j.azure-openai.dalle3.timeout", vals.get("quarkus.langchain4j.azure-openai.timeout"));

      return vals;
    }
  }
}
