package io.quarkus.sample.superheroes.narration.config.constraints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.config.NarrationConfig.OpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.OpenAIApiKeyValid.OpenAiApiKeyValidator;

public class OpenAiApiKeyValidatorTests {
  OpenAiApiKeyValidator validator = new OpenAiApiKeyValidator();
  OpenAI openAI = mock(OpenAI.class);

  @Test
  public void openAiDisabled() {
    when(this.openAI.enabled())
      .thenReturn(false);

    assertThat(this.validator.isValid(this.openAI, null))
      .isTrue();
  }

  @Test
  public void apiKeyFoundNotEnabled() {
    when(this.openAI.enabled())
      .thenReturn(false);

    when(this.openAI.apiKey())
      .thenReturn(Optional.of("api-key"));

    assertThat(this.validator.isValid(this.openAI, null))
      .isTrue();
  }

  @Test
  public void isntValid() {
    when(this.openAI.enabled())
      .thenReturn(true);

    when(this.openAI.apiKey())
      .thenReturn(Optional.empty());

    assertThat(this.validator.isValid(this.openAI, null))
      .isFalse();
  }
}
