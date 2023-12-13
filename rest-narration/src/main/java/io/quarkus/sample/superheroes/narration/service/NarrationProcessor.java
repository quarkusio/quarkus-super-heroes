package io.quarkus.sample.superheroes.narration.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 * This class is only needed until https://github.com/quarkiverse/quarkus-langchain4j/issues/130 is implemented.
 */
@ApplicationScoped
public class NarrationProcessor {
  private final NarrationService narrationService;
  private final NarrationConfig narrationConfig;

	public NarrationProcessor(NarrationService narrationService, NarrationConfig narrationConfig) {
		this.narrationService = narrationService;
		this.narrationConfig = narrationConfig;
	}

	@WithSpan("NarrationProcessor.narrate")
  public String narrate(@SpanAttribute("arg.fight") Fight fight) {
    var narration = this.narrationConfig.makeLiveCalls() ?
           this.narrationService.narrate(fight) :
           performNarrationNoLiveCall(fight);

    Log.debugf("Narration for fight %s =\n%s", fight, narration);
    return narration;
  }

  private String performNarrationNoLiveCall(Fight fight) {
    Log.debug("NOT making a live call because narration.make-live-calls=false");
    return this.narrationService.narrateFallback(fight);
  }
}
