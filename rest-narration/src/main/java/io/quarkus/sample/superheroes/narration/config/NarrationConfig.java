package io.quarkus.sample.superheroes.narration.config;

import java.util.Map;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "narration")
public interface NarrationConfig {
  /**
   * Fallback narration
   */
  String fallbackNarration();

  /**
   * OpenAI configuration
   */
  AzureOpenAI azureOpenAi();

  interface AzureOpenAI {
    /**
     * Whether or not to enable to openAI integration.
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
}
