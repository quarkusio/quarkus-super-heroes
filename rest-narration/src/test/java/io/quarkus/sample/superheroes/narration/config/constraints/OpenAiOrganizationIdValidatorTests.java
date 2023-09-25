package io.quarkus.sample.superheroes.narration.config.constraints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.config.NarrationConfig.OpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.OpenAIApiOrganizationIdValid.OpenAiOrganizationIdValidator;

public class OpenAiOrganizationIdValidatorTests {
  OpenAiOrganizationIdValidator validator = new OpenAiOrganizationIdValidator();
  OpenAI openAI = mock(OpenAI.class);

  @Test
  public void openAiDisabled() {
    when(this.openAI.enabled())
      .thenReturn(false);

    assertThat(this.validator.isValid(this.openAI, null))
      .isTrue();
  }

  @Test
  public void organizationIdFoundNotEnabled() {
    when(this.openAI.enabled())
      .thenReturn(false);

    when(this.openAI.organizationId())
      .thenReturn(Optional.of("orgId"));

    assertThat(this.validator.isValid(this.openAI, null))
      .isTrue();
  }

  @Test
  public void isntValid() {
    when(this.openAI.enabled())
      .thenReturn(true);

    when(this.openAI.organizationId())
      .thenReturn(Optional.empty());

    assertThat(this.validator.isValid(this.openAI, null))
      .isFalse();
  }
}
