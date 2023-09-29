package io.quarkus.sample.superheroes.narration.service;

import io.quarkus.sample.superheroes.narration.Fight;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.smallrye.mutiny.Uni;

public sealed interface NarrationService permits DefaultNarrationService, OpenAINarrationServiceBase {
  Uni<String> narrate(@SpanAttribute("arg.fight") Fight fight);
}
