package io.quarkus.sample.superheroes.narration;

public record ImageGenerationRequest(String narration, String winnerPictureUrl, String loserPictureUrl) {
}
