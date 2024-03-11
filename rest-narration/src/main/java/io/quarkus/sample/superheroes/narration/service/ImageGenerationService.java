package io.quarkus.sample.superheroes.narration.service;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.FightImage;

import dev.langchain4j.model.image.ImageModel;
import io.quarkiverse.langchain4j.ModelName;

@ApplicationScoped
public class ImageGenerationService {
  private final ImageModel imageModel;

  public ImageGenerationService(@ModelName("dalle3") ImageModel imageModel) {
    this.imageModel = imageModel;
  }

  public FightImage generateImageForNarration(String narration) {
    Log.debugf("Generating image for narration: %s", narration);
    var image = this.imageModel.generate(narration).content();
    return new FightImage(image.url().toString(), image.revisedPrompt());
  }
}
