package io.quarkus.sample.superheroes.narration.config.constraints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.config.NarrationConfig.AzureOpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.AzureOpenAIKeyValid.AzureOpenAiKeyValidator;

public class AzureOpenAiKeyValidatorTests {
  AzureOpenAiKeyValidator validator = new AzureOpenAiKeyValidator();
  AzureOpenAI azureOpenAI = mock(AzureOpenAI.class);

  @Test
  public void azureOpenAiDisabled() {
    when(this.azureOpenAI.enabled())
      .thenReturn(false);

    assertThat(this.validator.isValid(this.azureOpenAI, null))
      .isTrue();
  }

  @Test
  public void keyFoundNotEnabled() {
    when(this.azureOpenAI.enabled())
      .thenReturn(false);

    when(this.azureOpenAI.key())
      .thenReturn(Optional.of("key"));

    assertThat(this.validator.isValid(this.azureOpenAI, null))
      .isTrue();
  }

  @Test
  public void isntValid() {
    when(this.azureOpenAI.enabled())
      .thenReturn(true);

    when(this.azureOpenAI.key())
      .thenReturn(Optional.empty());

    assertThat(this.validator.isValid(this.azureOpenAI, null))
      .isFalse();
  }
}
