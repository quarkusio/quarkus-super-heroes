package io.quarkus.sample.superheroes.narration;

import io.quarkus.runtime.annotations.RegisterForReflection;

import dev.langchain4j.model.openai.internal.image.ImageUsage;

// Workaround for https://github.com/quarkiverse/quarkus-langchain4j/pull/2567
// Remove once quarkus-langchain4j ships the fix.
@RegisterForReflection(
  targets = {
    ImageUsage.class,
    ImageUsage.Builder.class,
    ImageUsage.TokensDetails.class,
    ImageUsage.TokensDetails.TokensDetailsBuilder.class
  }
)
public class NativeImageReflectionConfiguration {
}
