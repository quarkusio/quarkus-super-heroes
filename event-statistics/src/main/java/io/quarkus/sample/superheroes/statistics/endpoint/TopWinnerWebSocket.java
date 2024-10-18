package io.quarkus.sample.superheroes.statistics.endpoint;

import io.quarkus.websockets.next.WebSocket;

import io.quarkus.sample.superheroes.statistics.domain.Score;

import io.smallrye.mutiny.Multi;

/**
 * WebSocket endpoint for the {@code /stats/winners} endpoint. Exposes the {@code winner-stats} channel over the socket to anyone listening.
 * <p>
 *   Uses constructor injection over field injection to show how it is done.
 * </p>
 * @see TopWinnerStatsChannelHolder
 */
@WebSocket(path = "/stats/winners")
public class TopWinnerWebSocket extends EventStatsWebSocket<Iterable<Score>> {
	private final TopWinnerStatsChannelHolder topWinnerStatsChannelHolder;

	public TopWinnerWebSocket(TopWinnerStatsChannelHolder topWinnerStatsChannelHolder) {
		this.topWinnerStatsChannelHolder = topWinnerStatsChannelHolder;
	}

  @Override
  protected Multi<Iterable<Score>> getStream() {
    return this.topWinnerStatsChannelHolder.getWinners();
  }
}
