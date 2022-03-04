package io.quarkus.sample.superheroes.statistics.listener;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;

import io.smallrye.mutiny.Multi;

/**
 * Consumer of {@link Fight} events from Kafka. There are 2 consumers for performing different aggregations. Each consumer writes out to its own in-memory channel.
 */
@ApplicationScoped
public class SuperStats {
	private static final Logger LOGGER = Logger.getLogger(SuperStats.class);

	private final Ranking topWinners = new Ranking(10);
	private final TeamStats stats = new TeamStats();

	/**
	 * Transforms the {@link Fight} stream into a stream of ratios. Each ratio indicates the running percentage of battles won by heroes.
	 * @param results The {@link Fight} continuous stream
	 * @return A continuous stream of percentages of battles won by heroes sent to the {@code team-stats} in-memory channel.
	 */
	@Incoming("fights")
	@Outgoing("team-stats")
	public Multi<TeamScore> computeTeamStats(Multi<Fight> results) {
		return results.map(this.stats::add)
			.invoke(score -> LOGGER.debugf("Fight received. Computed the team statistics: %s", score));
	}

	/**
	 * Transforms the {@link Fight} stream into a running stream of top winners.
	 * <p>
	 *   The incoming stream is first grouped by {@link Fight#getWinnerName}. Then the number of wins for that winner is computed.
	 * </p>
	 * @param results The {@link Fight} continuous stream
	 * @return A continuous stream of the top 10 winners and the number of wins for each winner
	 */
	@Incoming("fights")
	@Outgoing("winner-stats")
	public Multi<Iterable<Score>> computeTopWinners(Multi<Fight> results) {
		return results
			.group().by(Fight::getWinnerName)
			.flatMap(group ->
				group.onItem().scan(Score::new, this::incrementScore)
					.filter(score -> score.getName() != null)
			)
			.map(this.topWinners::onNewScore)
			.invoke(topScores -> LOGGER.debugf("Fight received. Computed the top winners: %s", topScores));
	}

	private Score incrementScore(Score score, Fight fight) {
		return new Score(fight.getWinnerName(), score.getScore() + 1);
	}

	TeamStats getTeamStats() {
		return this.stats;
	}
}
