package io.quarkus.sample.superheroes.narration.config;

import java.util.Map;
import java.util.Optional;

import io.quarkus.sample.superheroes.narration.config.constraints.AzureOpenAIEndpointValid;
import io.quarkus.sample.superheroes.narration.config.constraints.AzureOpenAIKeyValid;
import io.quarkus.sample.superheroes.narration.config.constraints.OpenAIApiKeyValid;
import io.quarkus.sample.superheroes.narration.config.constraints.OpenAIApiOrganizationIdValid;
import io.quarkus.sample.superheroes.narration.config.constraints.SingleOpenAIImplementation;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "narration")
@SingleOpenAIImplementation
public interface NarrationConfig {
  /**
   * Fallback narration
   */
  String fallbackNarration();

  /**
   * Azure OpenAI configuration
   */
  AzureOpenAI azureOpenAi();

  /**
   * OpenAI configuration
   */
  OpenAI openAi();

  @AzureOpenAIKeyValid
  @AzureOpenAIEndpointValid
  interface AzureOpenAI {
    /**
     * Whether or not to enable the Azure OpenAI integration.
     * Defaults to {@code false}.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The Azure cognitive services account key ({@code client.azureopenai.key}).
     * <p>
     *   Only required if {@link #enabled()} == true
     * </p>
     */
    Optional<String> key();

    /**
     * The Azure OpenAI Endpoint ({@code client.azureopenai.endpoint})
     * <p>
     *  Only required if {@link #enabled()} == true
     * </p>
     */
    Optional<String> endpoint();

    /**
     * The Azure cognitive services deployment name ({@code client.azureopenai.deploymentname})
     * <p>
     *  Only required if {@link #enabled()} == true
     * </p>
     * <p>
     *   Default is {@code csdeploy-super-heroes}
     * </p>
     */
    @WithDefault("csdeploy-super-heroes")
    String deploymentName();

    /**
     * Any additional properties to be passed into the config as-is
     */
    Map<String, String> additionalProperties();
  }

  @OpenAIApiKeyValid
  @OpenAIApiOrganizationIdValid
  interface OpenAI {
    /**
     * Whether or not to enable to OpenAI integration.
     * Defaults to {@code false}.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The OpenAI API key ({@code client.openai.key}).
     * <p>
     *   Only required if {@link #enabled()} == true
     * </p>
     */
    Optional<String> apiKey();

    /**
     * The OpenAI organization id
     * <p>
     *   Only required if {@link #enabled()} == true
     * </p>
     */
    Optional<String> organizationId();
  }
}
