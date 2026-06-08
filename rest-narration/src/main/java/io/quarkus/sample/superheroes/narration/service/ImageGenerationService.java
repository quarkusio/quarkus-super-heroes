package io.quarkus.sample.superheroes.narration.service;

import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.ImageGenerationRequest;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
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
  @UserMessage("""
    Here is the fight narration: "{request.narration}"
    
    ---------
    If possible, please use the pictures provided for the winner and loser:
    
    Winner picture URL: {request.winnerPictureUrl}
    Loser picture URL: {request.loserPictureUrl}
    """)
  Image generateImage(ImageGenerationRequest request);

  @WithSpan("ImageGenerationService.generateImageForNarration")
  default FightImage generateImageForNarration(@SpanAttribute("arg.request") ImageGenerationRequest request) {
    Log.debugf("Generating image for request: %s", request);

    var image = generateImage(request);
    var imageUrl = Optional.ofNullable(image.url())
      .map(URI::toString)
      .orElseGet(() -> "data:%s;base64,%s".formatted(Optional.ofNullable(image.mimeType()).orElse("image/png"), image.base64Data()));

    return new FightImage(imageUrl);
  }
}
