package io.quarkus.sample.superheroes.statistics.endpoint;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.sample.superheroes.statistics.domain.Score;

import io.smallrye.mutiny.Multi;

/**
 * Placeholder class to hold the {@code winner-stats} in-memory channel. Main purpose is to allow mocking of the channel in tests since there isn't any way in Quarkus to mock in-memory channels.
 * <p>
 *   Also uses field injection via {@link Inject @Inject} (instead of constructor injection) to show how it is done.
 * </p>
 * @see TopWinnerWebSocket
 */
@ApplicationScoped
class TopWinnerStatsChannelHolder {
	@Inject
	@Channel("winner-stats")
	Multi<Iterable<Score>> winners;

	Multi<Iterable<Score>> getWinners() {
		return this.winners;
	}
}
