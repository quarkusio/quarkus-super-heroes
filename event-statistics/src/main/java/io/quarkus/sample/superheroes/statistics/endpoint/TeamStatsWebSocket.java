package io.quarkus.sample.superheroes.statistics.endpoint;

import jakarta.inject.Inject;

import io.quarkus.websockets.next.WebSocket;

import io.quarkus.sample.superheroes.statistics.domain.TeamScore;

import io.smallrye.mutiny.Multi;

/**
 * WebSocket endpoint for the {@code /stats/team} endpoint. Exposes the {@code team-stats} channel over the socket to anyone listening.
 * <p>
 *   Uses field injection via {@link Inject @Inject} over construction injection to show how it is done
 * </p>
 * @see TeamStatsChannelHolder
 */
@WebSocket(path = "/stats/team")
public class TeamStatsWebSocket extends EventStatsWebSocket<TeamScore> {
	@Inject
	TeamStatsChannelHolder teamStatsChannelHolder;

  @Override
  protected Multi<TeamScore> getStream() {
    return this.teamStatsChannelHolder.getTeamStats();
  }
}
