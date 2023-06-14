package io.quarkus.sample.superheroes.ui;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.quarkus.runtime.annotations.RegisterForReflection;

/*
Why do we need to register this for reflection? Normally this would be automatic if
we return it from a REST endpoint, but because we're handling our own
object mapping, we need to do our own registering.
 */
@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
@RegisterForReflection
public record Config(String apiBaseUrl, boolean calculateApiBaseUrl) {
}
