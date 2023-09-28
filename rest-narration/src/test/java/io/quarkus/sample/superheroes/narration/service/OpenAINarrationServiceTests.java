package io.quarkus.sample.superheroes.narration.service;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
@TestProfile(OpenAINarrationServiceTests.OpenAITestProfile.class)
class OpenAINarrationServiceTests extends OpenAINarrationServiceTestsBase<OpenAINarrationService> {
  @InjectSpy
  OpenAINarrationService narrationService;

  @Override
  protected OpenAINarrationService getNarrationService() {
    return this.narrationService;
  }

  @Override
  protected String getModelId() {
    return "gpt-3.5-turbo";
  }

  @Override
  protected Duration getExpectedOperationTimeout() {
    return Duration.ofSeconds(30);
  }

  public static class OpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
        "narration.open-ai.enabled", "true",
        "narration.open-ai.api-key", "MY_KEY",
        "narration.open-ai.organization-id", "MY_ORG"
      );
    }
  }
}
