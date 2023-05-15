package io.quarkus.sample.superheroes.statistics.listener;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.quarkus.opentelemetry.runtime.QuarkusContextStorage;
import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.TracingMetadata;
import io.smallrye.reactive.messaging.annotations.Broadcast;

/**
 * Consumer of {@link Fight} events from Kafka. There are 2 consumers for performing different aggregations. Each consumer writes out to its own in-memory channel.
 */
@ApplicationScoped
public class SuperStats {
  private static final Logger LOGGER = Logger.getLogger(SuperStats.class);
  private static final String SPAN_KEY = "CURRENT_SPAN";
  private static final String MESSAGE_KEY = "CURRENT_MESSAGE";

  static final String FIGHTS_CHANNEL_NAME = "fights";
  static final String TEAM_STATS_CHANNEL_NAME = "team-stats";
  static final String TOP_WINNERS_CHANNEL_NAME = "winner-stats";

  private final Ranking topWinners = new Ranking(10);
  private final TeamStats stats = new TeamStats();

  private final Tracer tracer;
  private final MutinyEmitter<TeamScore> teamStatsEmitter;

  public SuperStats(Tracer tracer, @Broadcast @Channel(TEAM_STATS_CHANNEL_NAME) MutinyEmitter<TeamScore> teamStatsEmitter) {
    this.tracer = tracer;
    this.teamStatsEmitter = teamStatsEmitter;
  }

  /**
   * Processes a stream of {@link Fight}s. Computes {@link #computeTeamStats(io.quarkus.sample.superheroes.fight.schema.Fight) team stats}
   * and {@link #computeTopWinners(io.smallrye.mutiny.Multi) top winners}
   * @param fights
   * @return
   */
  @Incoming(FIGHTS_CHANNEL_NAME)
  @Outgoing(TOP_WINNERS_CHANNEL_NAME)
  @Broadcast
  public Multi<Message<Iterable<Score>>> processFight(Multi<Message<Fight>> fights) {
    return fights.withContext((multi, ctx) -> {
      var teamStatsMulti = createTracedMulti(multi, ctx)
        .call(this::computeTeamStats);

      return computeTopWinners(teamStatsMulti)
        .onItem().invoke(() -> closeSpanFromContext(ctx))
        .onFailure().invoke(() -> closeSpanFromContext(ctx))
        .map(scores -> scoresMessage(scores, ctx));
    });
  }

  /**
   * Transforms the {@link Fight} stream into a stream of {@link io.quarkus.sample.superheroes.statistics.domain.TeamScore scores}.
   * Each score indicates the running percentage of battles won by heroes.
   * @param fight The {@link Fight} to compute
   * @return A continuous stream of percentages of battles won by heroes sent to the {@code team-stats} in-memory channel.
   */
  @WithSpan("SuperStats.computeTeamStats")
  Uni<Void> computeTeamStats(@SpanAttribute("arg.fight") Fight fight) {
    LOGGER.debugf("[computeTeamStats] - Got message: %s", fight);

    return Uni.createFrom().item(() -> this.stats.add(fight))
      .invoke(score -> LOGGER.debugf("[computeTeamStats] - Computed the team statistics: %s", score))
      .chain(this.teamStatsEmitter::send);
  }

  /**
   * Transforms the {@link Fight} stream into a running stream of top winners.
   * <p>
   *   The incoming stream is first grouped by {@link Fight#getWinnerName}. Then the number of wins for that winner is computed.
   * </p>
   * @param fights The {@link Fight} continuous stream
   * @return A continuous stream of the top 10 winners and the number of wins for each winner
   */
  private Multi<Iterable<Score>> computeTopWinners(Multi<Fight> fights) {
    return fights
      .withContext((multi, ctx) -> {
          var spanName = "SuperStats.computeTopWinners";

          return multi
            .invoke(fight -> ctx.put(spanName, createChildSpan(spanName, fight, getParentSpan(ctx))))
            .invoke(fight -> LOGGER.debugf("[computeTopWinners] - Got message: %s", fight))
            .group().by(Fight::getWinnerName)
            .flatMap(group ->
              group.onItem().scan(Score::new, this::incrementScore)
                .filter(score -> score.name() != null)
            )
            .map(this.topWinners::onNewScore)
            .invoke(winners -> LOGGER.debugf("[computeTopWinners] - Computed the top winners: %s", winners))
            .onItem().invoke(() -> closeSpanFromContext(ctx, spanName))
            .onFailure().invoke(() -> closeSpanFromContext(ctx, spanName));
        }
      );
  }

  private Multi<Fight> createTracedMulti(Multi<Message<Fight>> fights, Context ctx) {
    return fights
      .map(m -> {
        ctx.put(MESSAGE_KEY, m);

        handleTrace(m, "SuperStats.processFight")
          .ifPresent(s -> {
            s.makeCurrent();
            ctx.put(SPAN_KEY, s);
          });

        return m.getPayload();
      });
  }

  private Score incrementScore(Score score, Fight fight) {
    return new Score(fight.getWinnerName(), score.score() + 1);
  }

  TeamStats getTeamStats() {
    return this.stats;
  }

  private static Message<Iterable<Score>> scoresMessage(Iterable<Score> scores, Context ctx) {
    Message<Fight> message = ctx.get(MESSAGE_KEY);
    ctx.delete(MESSAGE_KEY);
    return message.withPayload(scores);
  }

  private static void closeSpanFromContext(Context ctx, String spanName) {
    closeSpan(Optional.ofNullable(ctx.getOrElse(spanName, () -> null)));
    ctx.delete(spanName);
  }

  private static void closeSpanFromContext(Context ctx) {
    closeSpanFromContext(ctx, SPAN_KEY);
  }

  private static void closeSpan(Optional<Span> span) {
    span.ifPresent(Span::end);
  }

  private static Optional<Span> getParentSpan(Context ctx) {
    return Optional.ofNullable(ctx.getOrElse(SPAN_KEY, () -> null));
  }

  private Span createChildSpan(String spanName, Fight fight, Optional<Span> parentSpan) {
    return createSpan(spanName, fight, getOtelContext(parentSpan));
  }

  private Span createSpan(String spanName, Fight fight, Optional<io.opentelemetry.context.Context> parent) {
    var spanBuilder = this.tracer
      .spanBuilder(spanName)
      .setAttribute("arg.fight", fight.toString());

    parent.ifPresent(c -> spanBuilder.setParent(c));

    return spanBuilder.startSpan();
  }

  private static Optional<io.opentelemetry.context.Context> getOtelContext(Optional<Span> parentSpan) {
    return Optional.ofNullable(
      parentSpan
        .map(io.opentelemetry.context.Context.current()::with)
        .orElseGet(io.opentelemetry.context.Context::current)
    );
  }

  private Optional<Span> handleTrace(Message<Fight> message, String spanName) {
    return message.getMetadata(TracingMetadata.class)
      .map(metadata -> {
        QuarkusContextStorage.INSTANCE.attach(metadata.getCurrentContext());
        return createSpan(spanName, message.getPayload(), Optional.ofNullable(metadata.getCurrentContext()));
      });
  }
}
