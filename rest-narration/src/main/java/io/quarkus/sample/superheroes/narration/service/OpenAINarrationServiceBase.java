package io.quarkus.sample.superheroes.narration.service;

import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.SKBuilders;
import com.microsoft.semantickernel.orchestration.SKContext;
import com.microsoft.semantickernel.textcompletion.CompletionSKFunction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;

public abstract sealed class OpenAINarrationServiceBase implements NarrationService permits AzureOpenAINarrationService, OpenAINarrationService {
  private CompletionSKFunction narrateFunction = null;

  @Inject
  NarrationConfig narrationConfig;

  @Inject
  Instance<OpenAIAsyncClient> openAIAsyncClientInstance;

  protected abstract String getModelId();

  @PostConstruct
  void createResources() {
    Log.debugf("My implementation = %s", getClass().getName());

    // Creates an instance of the TextCompletion service
    Log.debug("Creating the ChatCompletion");
    var textCompletion = SKBuilders.chatCompletion()
      .withOpenAIClient(this.openAIAsyncClientInstance.get())
      .withModelId(getModelId())
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

  protected Uni<String> narrateFight(Fight fight) {
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

    var fightLocation = Optional.ofNullable(fight.location());
    fightContext.setVariable("location_name", fightLocation.map(FightLocation::name).orElse(""));
    fightContext.setVariable("location_description", fightLocation.map(FightLocation::description).orElse(""));

    var resultMono = this.narrateFunction.invokeAsync(fightContext);

    return UniReactorConverters.<SKContext>fromMono().from(resultMono)
      .onItem().ifNotNull().transform(SKContext::getResult)
      .invoke(narration -> Log.infof("The narration for the fight is: %s", narration));
  }

  Uni<String> fallbackNarrate(Fight fight) {
    return Uni.createFrom().item(this.narrationConfig.fallbackNarration())
      .invoke(() -> Log.warn("Falling back on Narration"));

  }
}
