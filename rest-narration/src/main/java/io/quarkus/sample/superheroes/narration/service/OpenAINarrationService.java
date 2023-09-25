package io.quarkus.sample.superheroes.narration.service;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;

import com.microsoft.semantickernel.connectors.ai.openai.util.ClientType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;

@LookupIfProperty(name = "narration.open-ai.enabled", stringValue = "true")
@ApplicationScoped
public class OpenAINarrationService extends OpenAINarrationServiceBase {
  private final NarrationConfig narrationConfig;

  public OpenAINarrationService(NarrationConfig narrationConfig) {
    super();
    this.narrationConfig = narrationConfig;
  }

  @Override
  @Timeout(value = 30, unit = ChronoUnit.SECONDS)
  @Fallback(fallbackMethod = "fallbackNarrate")
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
//  @CircuitBreakerName("narrate")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @WithSpan(kind = SpanKind.CLIENT, value="NarrationService.narrate")
  public Uni<String> narrate(@SpanAttribute("arg.fight") Fight fight) {
    return narrateFight(fight);
  }

  @Override
  protected String getModelId() {
    return "gpt-3.5-turbo";
  }

  @Override
  protected Map<String, String> getOpenAIProperties() {
    var requiredProps = Map.of(
      "client.openai.key", this.narrationConfig.openAi().apiKey().orElseThrow(() -> new IllegalArgumentException("Property 'narration.open-ai.api-key' property is not specified")),
      "client.openai.organizationid", this.narrationConfig.openAi().organizationId().orElseThrow(() -> new IllegalArgumentException("Property 'narration.open-ai.organization-id' property is not specified"))
    );

    var properties = new HashMap<String, String>();
    properties.putAll(requiredProps);
    properties.putAll(this.narrationConfig.openAi().additionalProperties());

    return properties;
  }

  @Override
  protected ClientType getClientType() {
    return ClientType.OPEN_AI;
  }

  @Override
  protected NarrationConfig getNarrationConfig() {
    return this.narrationConfig;
  }
}
