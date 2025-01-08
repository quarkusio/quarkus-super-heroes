package io.quarkus.sample.superheroes.narration.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.time.Duration;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;

@QuarkusTest
@ConnectWireMock
class NarrationServiceTests {
  private static final String EXPECTED_NARRATION = "In the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\n\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\n\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\n\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity.";

  private static final String REQUEST_JSON = """
    {
      "model": "gpt-4o-mini",
      "messages": [
        {
          "role": "system",
          "content": "You are a marvel comics writer, expert in all sorts of super heroes and super villains."
        },
        {
          "role": "user",
          "content": "Narrate the fight between a super hero and a super villain.\\n\\nDuring the narration, don't repeat \\"super hero\\" or \\"super villain\\".\\n\\nWrite 4 paragraphs maximum. Be creative.\\n\\nThe narration must be:\\n- G rated\\n- Workplace/family safe\\n- No sexism, racism, or other bias/bigotry\\n\\nHere is the data you will use for the winner:\\n\\n+++++\\nName: Han Solo\\nPowers: Big gun, doesn't believe in the force\\nLevel: 1000\\n+++++\\n\\nHere is the data you will use for the loser:\\n\\n+++++\\nName: Storm Trooper\\nPowers: Small gun\\nLevel: 500\\n+++++\\n\\nHere is the data you will use for the fight:\\n\\n+++++\\nHan Solo who is a Heroes has won the fight against Storm Trooper who is a Villains.\\n\\nThe fight took place in Gotham City, which can be described as An American city rife with corruption and crime, the home of its iconic protector Batman..\\n+++++\\n"
        }
      ],
      "temperature": 0.7,
      "top_p": 0.5,
      "presence_penalty": 0,
      "frequency_penalty": 0
    }
    """;

  private static final String RESPONSE_JSON = """
    {
      "id": "chatcmpl-8Uy1UC7ZFUXtjhYcY5VFj7eYGu6jX",
      "object": "chat.completion",
      "created": 1702391260,
      "model": "gpt-4o-mini-2024-07-18",
      "choices": [
        {
          "index": 0,
          "message": {
            "role": "assistant",
            "content": "In the gritty streets of Gotham City, a clash of epic proportions unfolded. Han Solo, a hero known for his sharpshooting skills and unwavering skepticism towards the force, faced off against Storm Trooper, a villain armed with nothing more than a small gun. The odds seemed stacked against the Storm Trooper, but he was determined to prove his worth.\\n\\nAs the battle commenced, Han Solo swiftly dodged the Storm Trooper's feeble shots, his agility and experience shining through. With a smirk on his face, Han Solo aimed his big gun with precision, firing shots that echoed through the city. The Storm Trooper, though outmatched, refused to back down, his determination fueling his every move.\\n\\nWith each passing moment, Han Solo's level of expertise became more apparent. His shots were calculated and deadly, while the Storm Trooper struggled to keep up. The hero's confidence grew, his movements becoming more fluid and effortless. It was clear that the Storm Trooper's small gun was no match for Han Solo's superior firepower.\\n\\nIn a final, decisive moment, Han Solo's shot found its mark, incapacitating the Storm Trooper. The hero emerged victorious, his unwavering resolve prevailing over the villain's futile attempts. As the city rejoiced in the triumph of justice, Han Solo stood tall, a symbol of hope and resilience in the face of adversity."
          },
          "finish_reason": "stop"
        }
      ],
      "usage": {
        "prompt_tokens": 232,
        "completion_tokens": 280,
        "total_tokens": 512
      },
      "system_fingerprint": null
    }
    """;

  private static final Fight FIGHT = new Fight(
    "Han Solo",
    1000,
    "Big gun, doesn't believe in the force",
    "Storm Trooper",
    500,
    "Small gun",
    "Heroes",
    "Villains",
    new FightLocation(
      "Gotham City",
      "An American city rife with corruption and crime, the home of its iconic protector Batman."
    )
  );

  @InjectSpy
  NarrationService narrationService;

  @ConfigProperty(name = "quarkus.langchain4j.openai.timeout")
  Duration timeout;

  WireMock wireMock;

  @BeforeEach
  void beforeEach() {
    this.wireMock.resetToDefaultMappings();
  }

  @Test
  void narrateSuccess() {
    this.wireMock.register(
      post(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(REQUEST_JSON, true, false))
        .willReturn(
          okJson(RESPONSE_JSON)
            .withHeader("openai-model", "gpt-3.5-turbo-0613")
            .withHeader("openai-organization", "my-org-1234")
            .withHeader("openai-version", "2020-10-01")
            .withHeader("openai-processing-ms", "15000")
        )
    );

    assertThat(this.narrationService.narrate(FIGHT))
      .isEqualTo(EXPECTED_NARRATION);

    this.wireMock.verifyThat(
      1,
      postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .withRequestBody(equalToJson(REQUEST_JSON, true, false))
    );

    verify(this.narrationService, never()).narrateFallback(any(Fight.class));
  }

  @Test
  void narrateFallback() {
    assertThat(this.narrationService.narrateFallback(FIGHT))
      .isEqualTo(NarrationService.FALLBACK_NARRATION);
  }

  @Test
  void narrateWithTimeout() {
    this.wireMock.register(
      post(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .willReturn(
          okJson(RESPONSE_JSON)
            .withHeader("openai-model", "gpt-3.5-turbo-0613")
            .withHeader("openai-organization", "my-org-1234")
            .withHeader("openai-version", "2020-10-01")
            .withHeader("openai-processing-ms", "15000")
            .withFixedDelay((int) this.timeout.multipliedBy(2).toMillis())
        )
    );

    assertThat(this.narrationService.narrate(FIGHT))
      .isEqualTo(NarrationService.FALLBACK_NARRATION);

    this.wireMock.verifyThat(
      2,
      postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
    );
  }

  @Test
  void narrateWithError() {
    this.wireMock.register(
      post(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
        .willReturn(serverError())
    );

    assertThat(this.narrationService.narrate(FIGHT))
      .isEqualTo(NarrationService.FALLBACK_NARRATION);

    this.wireMock.verifyThat(
      2,
      postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withHeader(HttpHeaders.ACCEPT, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.CONTENT_TYPE, equalToIgnoreCase(MediaType.APPLICATION_JSON))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer change-me"))
    );
  }
}
