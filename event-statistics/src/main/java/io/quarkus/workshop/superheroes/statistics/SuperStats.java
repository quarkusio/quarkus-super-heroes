package io.quarkus.workshop.superheroes.statistics;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SuperStats {

    @Inject
    Logger logger;

    private final Ranking topWinners = new Ranking(10);
    private final TeamStats stats = new TeamStats();

    @Incoming("fights")
    @Outgoing("team-stats")
    public Multi<Double> computeTeamStats(Multi<Fight> results) {
        return results
            .onItem().transform(stats::add)
            .invoke(() -> logger.info("Fight received. Computed the team statistics"));
    }

    @Incoming("fights")
    @Outgoing("winner-stats")
    public Multi<Iterable<Score>> computeTopWinners(Multi<Fight> results) {
        return results
            .group().by(fight -> fight.winnerName)
            .onItem().transformToMultiAndMerge(group ->
                group
                    .onItem().scan(Score::new, this::incrementScore))
            .onItem().transform(topWinners::onNewScore)
            .invoke(() -> logger.info("Fight received. Computed the top winners"));
    }

    private Score incrementScore(Score score, Fight fight) {
        score.name = fight.winnerName;
        score.score = score.score + 1;
        return score;
    }

}
