package io.quarkus.sample.superheroes.narration.service;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface NarrationService {
  @SystemMessage("You are a marvel comics writer, expert in all sorts of super heroes and super villains.")
  @UserMessage("""
    Narrate the fight between a super hero and a super villain.

    During the narration, don't repeat "super hero" or "super villain".
    
    Write 4 paragraphs maximum. Be creative.
    
    The narration must be:
    - G rated
    - Workplace/family safe
    - No sexism, racism, or other bias/bigotry
    
    Here is the data you will use for the winner:
    
    +++++
    Name: {fight.winnerName}
    Powers: {fight.winnerPowers}
    Level: {fight.winnerLevel}
    +++++
    
    Here is the data you will use for the loser:
    
    +++++
    Name: {fight.loserName}
    Powers: {fight.loserPowers}
    Level: {fight.loserLevel}
    +++++
    
    Here is the data you will use for the fight:
    
    +++++
    {fight.winnerName} who is a {fight.winnerTeam} has won the fight against {fight.loserName} who is a {fight.loserTeam}.
    
    The fight took place in {fight.location.name}, which can be described as {fight.location.description}.
    +++++
    """)
  @Fallback(fallbackMethod = "narrateFallback")
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  String narrate(@SpanAttribute("arg.fight") Fight fight);

  default String narrateFallback(@SpanAttribute("arg.fight") Fight fight) {
    return CDI.current().select(NarrationConfig.class).get().fallbackNarration();
  }
}
