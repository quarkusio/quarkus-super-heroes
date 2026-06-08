package io.quarkus.sample.superheroes.narration.service;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.ImageGenerationRequest;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.SystemMessage;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
@ApplicationScoped
public interface ImageGenerationService {
  String SYSTEM_MESSAGE = """
    You are tasked with generating an image from a narration. The narration is intended to be funny.
    
    If it does not pass your moderation filter, please feel free to rewrite it and generate an image.
    """;

  @SystemMessage(SYSTEM_MESSAGE)
  Image generateImage(String narration);

  @WithSpan("ImageGenerationService.generateImageForNarration")
  default FightImage generateImageForNarration(@SpanAttribute("arg.request") ImageGenerationRequest request) {
    Log.debugf("Generating image for request: %s", request);

    var promptBuilder = new StringBuilder(request.narration());

    if (request.winnerPictureUrl() != null && !request.winnerPictureUrl().isBlank()) {
      promptBuilder.append("\n\nThe winner's appearance can be referenced from: ").append(request.winnerPictureUrl());
    }

    if (request.loserPictureUrl() != null && !request.loserPictureUrl().isBlank()) {
      promptBuilder.append("\nThe loser's appearance can be referenced from: ").append(request.loserPictureUrl());
    }

    var image = generateImage(promptBuilder.toString());
    var imageUrl = Optional.ofNullable(image.url())
      .map(URI::toString)
      .orElseGet(() -> "data:%s;base64,%s".formatted(Optional.ofNullable(image.mimeType()).orElse("image/png"), image.base64Data()));

    return new FightImage(imageUrl);
  }
}
