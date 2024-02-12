package io.quarkus.sample.superheroes.narration.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.util.Map;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.condition.EnabledIf;

import io.quarkus.sample.superheroes.narration.rest.AzureOpenAiNarrationResourceIT.WiremockAzureOpenAITestProfile;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import io.quarkiverse.wiremock.devservice.WireMockConfigKey;

@QuarkusIntegrationTest
@TestProfile(WiremockAzureOpenAITestProfile.class)
@EnabledIf(value = "azureOpenAiEnabled", disabledReason = "Azure OpenAI profile is not enabled")
public class AzureOpenAiNarrationResourceIT extends NarrationResourceIT {
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
        .withRequestBody(equalToJson(REQUEST_JSON, true, false))
    );
	}

	@Override
	void shouldGetAFallbackOnError() {
		this.wireMock.register(
      post(urlPathEqualTo("/v1/chat/completions"))
	      .withQueryParam("api-version", matching(".+"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader("api-key", equalTo("change-me"))
        .withRequestBody(equalToJson(REQUEST_JSON, true, false))
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
        .withRequestBody(equalToJson(REQUEST_JSON, true, false))
    );
	}

	public static class WiremockAzureOpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
	    var hostname = Boolean.getBoolean("quarkus.container-image.build") ? "host.docker.internal" : "localhost";

      return Map.of(
				"narration.make-live-calls", "true",
        "quarkus.langchain4j.azure-openai.log-requests", "true",
        "quarkus.langchain4j.azure-openai.log-responses", "true",
        "quarkus.langchain4j.azure-openai.endpoint", "http://%s:${%s}/v1/".formatted(hostname, WireMockConfigKey.PORT),
        "quarkus.langchain4j.azure-openai.max-retries", "2",
        "quarkus.langchain4j.azure-openai.timeout", "3s"
      );
    }
  }
}
