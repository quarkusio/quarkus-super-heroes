package io.quarkus.sample.superheroes.statistics.endpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.quarkus.sample.superheroes.statistics.domain.Score;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class WinnerStatsChannelHolder {
	@Inject
	@Channel("winner-stats")
	Multi<Iterable<Score>> winners;

	public Multi<Iterable<Score>> getWinners() {
		return this.winners;
	}
}
