package io.quarkus.sample.superheroes.narration;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record FightImage(String imageUrl) {
}
