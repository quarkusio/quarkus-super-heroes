package io.quarkus.sample.superheroes.narration.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class NarrationConfigTests {
  @Inject
  NarrationConfig narrationConfig;

  @Test
  public void openAIDisabled() {
    assertThat(this.narrationConfig.azureOpenAi().enabled())
      .isFalse();
  }
}
