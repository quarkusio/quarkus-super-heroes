package io.quarkus.sample.superheroes.statistics.endpoint;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.unchecked.Unchecked;

/**
 * Base WebSocket endpoint which handles caching the stream and replaying
 * the last event from a stream upon subscription by new socket clients
 * @param <V> The object type inside each event
 */
public abstract class EventStatsWebSocket<V> {
  private final ConcurrentMap<Session, Cancellable> sessions = new ConcurrentHashMap<>();
  private Multi<String> cachedStream;
  private Logger logger;

  @Inject
  ObjectMapper mapper;

  protected abstract Multi<V> getStream();

  @OnOpen
  public void onOpen(Session session) {
    this.logger.debugf("Opening session with id %s", session.getId());
    this.sessions.put(session, createSubscription(session));
  }

  @OnClose
  public void onClose(Session session) {
    this.logger.debugf("Closing session with id %s", session.getId());
    Optional.ofNullable(this.sessions.remove(session))
      .ifPresent(Cancellable::cancel);
  }

  @PostConstruct
  public void initialize() {
    this.logger = Logger.getLogger(getClass());
    this.cachedStream = Multi.createBy().replaying().upTo(1).ofMulti(getStream())
      .map(Unchecked.function(this.mapper::writeValueAsString));
  }

  @PreDestroy
  public void cleanup() {
    this.sessions.forEach((session, subscription) -> {
      subscription.cancel();

      if (session.isOpen()) {
        try {
          session.close(new CloseReason(CloseCodes.GOING_AWAY, "Server shutting down"));
        }
        catch (IOException ex) {
          this.logger.errorf(ex, "Got exception (%s) while closing session", ex.getClass().getName());
        }
      }
    });
  }

  private Cancellable createSubscription(Session session) {
    return Optional.ofNullable(this.cachedStream)
      .orElseThrow(() -> new IllegalArgumentException("Cached stream (Multi<String>) has not been created. Please initialize it inside an @PostConstruct method."))
      .subscribe().with(serialized -> write(session, serialized));
  }

  private void write(Session session, String text) {
    this.logger.infof("[Session %s] - Writing message %s", session.getId(), text);

    session.getAsyncRemote().sendText(text, result -> {
      if (result.getException() != null) {
        this.logger.error("Unable to write message to web socket", result.getException());
      }
    });
  }
}
