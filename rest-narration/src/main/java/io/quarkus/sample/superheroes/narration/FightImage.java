package io.quarkus.sample.superheroes.narration;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "The generated image from the narration")
public record FightImage(String imageUrl, String imageNarration) {
}
