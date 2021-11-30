package io.quarkus.sample.superheroes.statistics.endpoint;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
class TeamStatsChannelHolder {
	@Inject
	@Channel("team-stats")
	Multi<Double> teamStats;

	Multi<Double> getTeamStats() {
		return this.teamStats;
	}
}
