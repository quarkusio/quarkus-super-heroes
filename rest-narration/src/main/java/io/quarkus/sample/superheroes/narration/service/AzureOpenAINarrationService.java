package io.quarkus.sample.superheroes.narration.service;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.connectors.ai.openai.util.ClientType;
import com.microsoft.semantickernel.connectors.ai.openai.util.OpenAIClientProvider;
import com.microsoft.semantickernel.exceptions.ConfigurationException;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;

@LookupIfProperty(name = "narration.azure-open-ai.enabled", stringValue = "true")
@ApplicationScoped
public class AzureOpenAINarrationService implements NarrationService {
  private final NarrationConfig narrationConfig;
  private CompletionSKFunction narrateFunction = null;

  public AzureOpenAINarrationService(NarrationConfig narrationConfig) {
    this.narrationConfig = narrationConfig;
  }

  @PostConstruct
  void createAzureResources() {
    Log.debug("Creating OpenAIAsyncClient");
    var client = getClient(this.narrationConfig);

    // Creates an instance of the TextCompletion service
    Log.debug("Creating the ChatCompletion");
    var textCompletion = SKBuilders.chatCompletion()
      .withOpenAIClient(client)
      .withModelId(this.narrationConfig.azureOpenAi().deploymentName())
      .build();

    // Instantiates the Kernel
    Log.debug("Creating the Kernel");
    var kernel = SKBuilders.kernel()
      .withDefaultAIService(textCompletion)
      .build();

    // Registers skills
    Log.debug("Creating the skill");
    var skill = kernel.importSkillFromResources("skills", "NarrationSkill",  "NarrateFight");

    Log.debug("Creating the NarrateFight function");
    this.narrateFunction = Optional.ofNullable(skill.getFunction("NarrateFight", CompletionSKFunction.class))
      .orElseThrow(() -> new IllegalArgumentException("Not able to create the narrate function. Please inspect your configuration."));
  }

  @Override
  @Timeout(value = 10, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackNarrate")
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("narrate")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @WithSpan(kind = SpanKind.CLIENT, value="NarrationService.narrate")
  public Uni<String> narrate(@SpanAttribute("arg.fight") Fight fight) {
    // Ask to narrate a fight
    var fightContext = SKBuilders.context().build();
    fightContext.setVariable("winner_team", fight.winnerTeam());
    fightContext.setVariable("winner_name", fight.winnerName());
    fightContext.setVariable("winner_powers", fight.winnerPowers());
    fightContext.setVariable("winner_level", String.valueOf(fight.winnerLevel()));
    fightContext.setVariable("loser_team", fight.loserTeam());
    fightContext.setVariable("loser_name", fight.loserName());
    fightContext.setVariable("loser_powers", fight.loserPowers());
    fightContext.setVariable("loser_level", String.valueOf(fight.loserLevel()));

    var resultMono = this.narrateFunction.invokeAsync(fightContext);

    return UniReactorConverters.<SKContext>fromMono().from(resultMono)
      .onItem().ifNotNull().transform(SKContext::getResult)
      .invoke(narration -> Log.infof("The narration for the fight is: %s", narration));
  }

  Uni<String> fallbackNarrate(Fight fight) {
    return Uni.createFrom().item(this.narrationConfig.fallbackNarration())
      .invoke(() -> Log.warn("Falling back on Narration"));

  }

  private OpenAIAsyncClient getClient(NarrationConfig narrationConfig) {
    var requiredProps = Map.of(
      "client.azureopenai.key", narrationConfig.azureOpenAi().key().orElseThrow(() -> new IllegalArgumentException("Property 'narration.azure-open-ai.key' property is not specified")),
      "client.azureopenai.endpoint", narrationConfig.azureOpenAi().endpoint().orElseThrow(() -> new IllegalArgumentException("Property 'narration.azure-open-ai.endpoint' property is not specified")),
      "client.azureopenai.deploymentname", narrationConfig.azureOpenAi().deploymentName()
    );

    var properties = new HashMap<String, String>();
    properties.putAll(requiredProps);
    properties.putAll(narrationConfig.azureOpenAi().additionalProperties());

    try {
      return new OpenAIClientProvider(properties, ClientType.AZURE_OPEN_AI)
        .getAsyncClient();
    }
    catch (ConfigurationException e) {
      throw new RuntimeException(e);
    }
  }
}
