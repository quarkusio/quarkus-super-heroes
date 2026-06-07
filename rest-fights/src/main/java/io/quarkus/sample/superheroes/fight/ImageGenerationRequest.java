package io.quarkus.sample.superheroes.fight;

import jakarta.validation.constraints.NotBlank;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request for image generation from a narration")
public record ImageGenerationRequest(@NotBlank String narration, String winnerPictureUrl, String loserPictureUrl) {
}
