package io.quarkus.sample.superheroes.statistics.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Data class for a score
 * <p>
 *   The {@link RegisterForReflection @RegisterForReflection} annotation instructs the native compilation to allow reflection access to the class. Without it, the serialization/deserialization would not work when running the native executable.
 * </p>
 */
@RegisterForReflection
public record Score(String name, int score) {
	public Score() {
		this(null, 0);
	}
}
