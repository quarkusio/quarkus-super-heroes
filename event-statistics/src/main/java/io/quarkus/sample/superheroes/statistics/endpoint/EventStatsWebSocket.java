package io.quarkus.sample.superheroes.statistics.endpoint;

import jakarta.annotation.PostConstruct;

import org.jboss.logging.Logger;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnPongMessage;
import io.quarkus.websockets.next.WebSocketConnection;

import io.smallrye.mutiny.Multi;
import io.vertx.core.buffer.Buffer;

/**
 * Base WebSocket endpoint which handles caching the stream and replaying
 * the last event from a stream upon subscription by new socket clients
 * @param <V> The object type inside each event
 */
public abstract class EventStatsWebSocket<V> {
  private Logger logger;

  protected abstract Multi<V> getStream();

  @OnOpen
  public Multi<V> onOpen(WebSocketConnection connection) {
    this.logger.debugf("Opening connection with id %s", connection.id());

    return Multi.createBy()
      .replaying()
      .upTo(1)
      .ofMulti(getStream())
      .invoke(v -> this.logger.infof("[Connection %s] - Writing message %s", connection.id(), v));
  }

  @OnPongMessage
  public void onPongMessage(WebSocketConnection connection, Buffer pongMessage) {
    this.logger.debugf("Got pong message (%s) on %s from connection %s", pongMessage.toString(), connection.handshakeRequest().path(), connection.id());
  }

  @OnClose
  public void onClose(WebSocketConnection connection) {
    this.logger.debugf("Closing connection with id %s", connection.id());
  }

  @PostConstruct
  public void initialize() {
    this.logger = Logger.getLogger(getClass());
  }
}
