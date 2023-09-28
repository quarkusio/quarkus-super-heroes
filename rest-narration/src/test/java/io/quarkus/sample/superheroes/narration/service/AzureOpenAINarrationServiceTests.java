package io.quarkus.sample.superheroes.narration.service;

import java.time.Duration;
import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
@TestProfile(AzureOpenAINarrationServiceTests.AzureOpenAITestProfile.class)
class AzureOpenAINarrationServiceTests extends OpenAINarrationServiceTestsBase<AzureOpenAINarrationService> {
  @InjectSpy
  AzureOpenAINarrationService narrationService;

  @Override
  protected AzureOpenAINarrationService getNarrationService() {
    return this.narrationService;
  }

  @Override
  protected String getModelId() {
    return this.narrationConfig.azureOpenAi().deploymentName();
  }

  @Override
  protected Duration getExpectedOperationTimeout() {
    return Duration.ofSeconds(10);
  }

  public static class AzureOpenAITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
        "narration.azure-open-ai.enabled", "true",
        "narration.azure-open-ai.key", "MY_KEY",
        "narration.azure-open-ai.endpoint", "MY_ENDPOINT"
      );
    }
  }
}
