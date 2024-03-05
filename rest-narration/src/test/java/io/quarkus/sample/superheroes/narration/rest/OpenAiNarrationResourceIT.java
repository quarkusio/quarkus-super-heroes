package io.quarkus.sample.superheroes.narration.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import io.quarkus.sample.superheroes.narration.rest.OpenAiNarrationResourceIT.WiremockOpenAITestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import io.quarkiverse.wiremock.devservice.WireMockConfigKey;

@QuarkusIntegrationTest
@TestProfile(WiremockOpenAITestProfile.class)
@DisabledIf(value = "azureOpenAiEnabled", disabledReason = "Azure OpenAI profile is enabled")
class OpenAiNarrationResourceIT extends NarrationResourceIT {
  @Test
	@Override
	void shouldNarrateAFight() {
		super.shouldNarrateAFight();

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
  @Override
  void shouldGenerateAnImageFromNarration() {
    super.shouldGenerateAnImageFromNarration();

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
	@Override
	void shouldGetAFallbackOnError() {
		this.wireMock.register(
      post(urlPathEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
        .willReturn(serverError())
    );

		super.shouldGetAFallbackOnError();

		this.wireMock.verifyThat(
      2,
      postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(NARRATION_REQUEST_JSON, true, false))
    );
	}

	public static class WiremockOpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
	    var hostname = Boolean.getBoolean("quarkus.container-image.build") ? "host.docker.internal" : "localhost";

      var vals =  new HashMap<>(Map.of(
				"quarkus.langchain4j.openai.enable-integration", "true",
        "quarkus.langchain4j.openai.log-requests", "true",
        "quarkus.langchain4j.openai.log-responses", "true",
        "quarkus.langchain4j.openai.base-url", "http://%s:${%s}/v1/".formatted(hostname, WireMockConfigKey.PORT),
        "quarkus.langchain4j.openai.max-retries", "2",
        "quarkus.langchain4j.openai.timeout", "3s"
      ));

      vals.put("quarkus.langchain4j.openai.dalle3.enable-integration", vals.get("quarkus.langchain4j.openai.enable-integration"));
      vals.put("quarkus.langchain4j.openai.dalle3.log-requests", vals.get("quarkus.langchain4j.openai.log-requests"));
      vals.put("quarkus.langchain4j.openai.dalle3.log-responses", vals.get("quarkus.langchain4j.openai.log-responses"));
      vals.put("quarkus.langchain4j.openai.dalle3.base-url", vals.get("quarkus.langchain4j.openai.base-url"));
      vals.put("quarkus.langchain4j.openai.dalle3.max-retries", vals.get("quarkus.langchain4j.openai.max-retries"));
      vals.put("quarkus.langchain4j.openai.dalle3.timeout", vals.get("quarkus.langchain4j.openai.timeout"));

      return vals;
    }
  }
}
