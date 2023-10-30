package io.quarkus.sample.superheroes.narration.service;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig.AzureOpenAI;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig.OpenAI;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.connectors.ai.openai.util.ClientType;
import com.microsoft.semantickernel.connectors.ai.openai.util.OpenAIClientProvider;
import com.microsoft.semantickernel.exceptions.ConfigurationException;

public class OpenAIAsyncClientProvider {
  @Produces
  @ApplicationScoped
  @LookupIfProperty(name = "narration.azure-open-ai.enabled", stringValue = "true")
  public OpenAIAsyncClient azureOpenAIAsyncClient(NarrationConfig narrationConfig) throws ConfigurationException {
    return new OpenAIClientProvider(getAzureOpenAIProperties(narrationConfig.azureOpenAi()), ClientType.AZURE_OPEN_AI)
      .getAsyncClient();
  }

  @Produces
  @ApplicationScoped
  @LookupIfProperty(name = "narration.open-ai.enabled", stringValue = "true")
  public OpenAIAsyncClient openAIAsyncClient(NarrationConfig narrationConfig) throws ConfigurationException {
    return new OpenAIClientProvider(getOpenAIProperties(narrationConfig.openAi()), ClientType.OPEN_AI)
      .getAsyncClient();
  }

  private Map<String, String> getAzureOpenAIProperties(AzureOpenAI azureOpenAI) {
    var requiredProps = Map.of(
      "client.azureopenai.key", azureOpenAI.key().orElseThrow(() -> new IllegalArgumentException("Property 'narration.azure-open-ai.key' property is not specified")),
      "client.azureopenai.endpoint", azureOpenAI.endpoint().orElseThrow(() -> new IllegalArgumentException("Property 'narration.azure-open-ai.endpoint' property is not specified")),
      "client.azureopenai.deploymentname", azureOpenAI.deploymentName()
    );

    var properties = new HashMap<String, String>();
    properties.putAll(requiredProps);
    properties.putAll(azureOpenAI.additionalProperties());

    return properties;
  }

  private Map<String, String> getOpenAIProperties(OpenAI openAI) {
    var requiredProps = Map.of(
      "client.openai.key", openAI.apiKey().orElseThrow(() -> new IllegalArgumentException("Property 'narration.open-ai.api-key' property is not specified")),
      "client.openai.organizationid", openAI.organizationId().orElseThrow(() -> new IllegalArgumentException("Property 'narration.open-ai.organization-id' property is not specified"))
    );

    var properties = new HashMap<String, String>();
    properties.putAll(requiredProps);
    properties.putAll(openAI.additionalProperties());

    return properties;
  }
}
