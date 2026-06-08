package io.quarkus.sample.superheroes.fight.client;

public record NarrationImageGenerationRequest(String narration, String winnerPictureUrl, String loserPictureUrl) {
}
