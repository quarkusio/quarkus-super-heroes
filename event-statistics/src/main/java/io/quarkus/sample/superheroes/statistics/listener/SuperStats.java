package io.quarkus.sample.superheroes.statistics.listener;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.quarkus.sample.superheroes.statistics.domain.Fight;
import io.quarkus.sample.superheroes.statistics.domain.Ranking;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamStats;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class SuperStats {
	private static final Logger LOGGER = Logger.getLogger(SuperStats.class);

	private final Ranking topWinners = new Ranking(10);
	private final TeamStats stats = new TeamStats();

	@Incoming("fights")
	@Outgoing("team-stats")
	public Multi<Double> computeTeamStats(Multi<Fight> results) {
		return results.map(this.stats::add)
			.invoke(stats -> LOGGER.infof("Fight received. Computed the team statistics: %d", stats));
	}

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
			.invoke(topScores -> LOGGER.infof("Fight received. Computed the top winners: %s", topScores));
	}

	private Score incrementScore(Score score, Fight fight) {
		return new Score(fight.getWinnerName(), score.getScore() + 1);
	}

	TeamStats getTeamStats() {
		return this.stats;
	}
}
