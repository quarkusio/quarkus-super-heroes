package io.quarkus.sample.superheroes.narration.config.constraints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.ParameterizedTest.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig.AzureOpenAI;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig.OpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.SingleOpenAIImplementation.SingleOpenAIImplementationValidator;

public class SingleOpenAIImplementationValidatorTests {
  SingleOpenAIImplementationValidator validator = new SingleOpenAIImplementationValidator();
  NarrationConfig narrationConfig = mock(NarrationConfig.class);
  AzureOpenAI azureOpenAI = mock(AzureOpenAI.class);
  OpenAI openAI = mock(OpenAI.class);

  @BeforeEach
  public void beforeEach() {
    when(this.narrationConfig.azureOpenAi())
      .thenReturn(this.azureOpenAI);

    when(this.narrationConfig.openAi())
      .thenReturn(this.openAI);
  }

  @ParameterizedTest(name = DISPLAY_NAME_PLACEHOLDER + "[" + INDEX_PLACEHOLDER + "] (" + ARGUMENTS_WITH_NAMES_PLACEHOLDER + ")")
  @MethodSource("isValidSource")
  public void isValid(boolean azureOpenAiEnabled, boolean openAiEnabled) {
    when(this.azureOpenAI.enabled())
      .thenReturn(azureOpenAiEnabled);

    when(this.openAI.enabled())
      .thenReturn(openAiEnabled);

    assertThat(this.validator.isValid(this.narrationConfig, null))
      .isTrue();
  }

  @Test
  public void isntValid() {
    when(this.azureOpenAI.enabled())
      .thenReturn(true);

    when(this.openAI.enabled())
      .thenReturn(true);

    assertThat(this.validator.isValid(this.narrationConfig, null))
      .isFalse();
  }

  static Stream<Arguments> isValidSource() {
    return Stream.of(
      Arguments.of(false, false),
      Arguments.of(true, false),
      Arguments.of(false, true)
    );
  }
}
