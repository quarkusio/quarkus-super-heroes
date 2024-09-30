package io.quarkus.sample.superheroes.narration.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.FightImage;

import dev.langchain4j.data.image.Image;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(modelName = "dalle3")
@ApplicationScoped
public interface ImageGenerationService {
  Image generateImage(String narration);

  @WithSpan("ImageGenerationService.generateImageForNarration")
  default FightImage generateImageForNarration(@SpanAttribute("arg.narration") String narration) {
    Log.debugf("Generating image for narration: %s", narration);
    var image = generateImage(narration);

    return new FightImage(image.url().toString(), image.revisedPrompt());
  }
}
