package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.logging.Log;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.BasicWebSocketConnector.ExecutionModel;
import io.quarkus.websockets.next.WebSocketClientConnection;

import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import lombok.extern.slf4j.Slf4j;

/**
 * Tests for the {@link TopWinnerWebSocket} and {@link TeamStatsWebSocket} classes.
 * <p>
 *   These tests mock the {@link TopWinnerStatsChannelHolder#getWinners()} and {@link TeamStatsChannelHolder#getTeamStats()} methods to return pre-defined input and then set up a sample WebSocket client to listen to messages sent by the server. Each message received is placed into a {@link java.util.concurrent.BlockingQueue} so that message content can be asserted once the expected number of messages have been received.
 * </p>
 */
@Slf4j
@QuarkusTest
class WebSocketsTests {
  @TestHTTPResource("/stats/winners")
  URI topWinnersUri;

  @InjectMock
  TopWinnerStatsChannelHolder topWinnerStatsChannelHolder;

  @TestHTTPResource("/stats/team")
  URI teamStatsUri;

  @InjectMock
  TeamStatsChannelHolder teamStatsChannelHolder;

  @Inject
  ObjectMapper objectMapper;

  @Inject
  Instance<BasicWebSocketConnector> connectorInstance;

  @Test
  void testScenarios() {
    // Set up the Queues to handle the messages
    var teamStatsMessages = new LinkedBlockingQueue<String>();
    var topWinnerMessages = new LinkedBlockingQueue<String>();

    // Set up a single consumer latch for each websocket
    // It will wait for the client to connect and subscribe to the stream before emitting items
    var teamStatsLatch = createLatch();
    var topWinnersLatch = createLatch();
    var teamStatsDelayedUni = createDelayedUni(teamStatsLatch);
    var topWinnersDelayedUni = createDelayedUni(topWinnersLatch);
    var delayedTeamStatsMulti = createDelayedMulti(teamStatsDelayedUni, WebSocketsTests::createTeamStatsItems);
    var delayedTopWinnerMulti = createDelayedMulti(topWinnersDelayedUni, WebSocketsTests::createTopWinnerItems);

    // Mock TeamStatsChannelHolder.getTeamStats() to return the delayed Multi
    when(this.teamStatsChannelHolder.getTeamStats()).thenReturn(delayedTeamStatsMulti);

    // Mock TopWinnerStatsChannelHolder.getWinners() to return the delayed Multi
    when(this.topWinnerStatsChannelHolder.getWinners()).thenReturn(delayedTopWinnerMulti);

    var teamStatsClient = new WebsocketTestClient("team", this.teamStatsUri, teamStatsMessages, this.connectorInstance.get(), teamStatsLatch);
    var topWinnerClient = new WebsocketTestClient("winners", this.topWinnersUri, topWinnerMessages, this.connectorInstance.get(), topWinnersLatch);
    teamStatsClient.connect();
    topWinnerClient.connect();

    var expectedTeamStats = createTeamStatsItems().toList();
    var expectedTopWinners = createTopWinnerItems().toList();

    // Wait for our messages to appear in the queue
    await()
      .atMost(Duration.ofMinutes(5))
      .pollInterval(Duration.ofSeconds(10))
      .until(() -> (teamStatsMessages.size() == expectedTeamStats.size()) && (topWinnerMessages.size() == expectedTopWinners.size()));

    validateTeamStats(teamStatsMessages, expectedTeamStats);
    validateTopWinnerStats(topWinnerMessages, expectedTopWinners);

    // Close the connections
    teamStatsClient.close();
    topWinnerClient.close();
  }

  private void validateTeamStats(BlockingQueue<String> teamStatsMessages, List<TeamScore> expectedItems) {
    System.out.println("Team Stats Messages received by test: " + teamStatsMessages);

    // Perform assertions that all expected messages were received
    expectedItems.stream()
      .map(Unchecked.function(this.objectMapper::writeValueAsString))
      .forEach(expectedMsg ->
        assertThat(teamStatsMessages.poll())
          .isNotNull()
          .isEqualTo(expectedMsg)
      );
  }

  private void validateTopWinnerStats(BlockingQueue<String> topWinnerMessages, List<Iterable<Score>> expectedItems) {
    System.out.println("Top Winner Messages received by test: " + topWinnerMessages);

    // Perform assertions that all expected messages were received
    expectedItems.stream()
      .map(Unchecked.function(this.objectMapper::writeValueAsString))
      .forEach(expectedMsg ->
        assertThat(topWinnerMessages.poll())
          .isNotNull()
          .isEqualTo(expectedMsg)
      );
  }

  private static <T> Multi<T> createDelayedMulti(Uni<Void> delayedUni, Supplier<Stream<T>> itemsProducer) {
    return Multi.createFrom().items(itemsProducer)
      .onItem().call(item -> Uni.createFrom().nullItem().onItem().delayIt().until(o -> delayedUni));
  }

  private static Uni<Void> createDelayedUni(CountDownLatch latch) {
    return Uni.createFrom().voidItem()
      .onItem().delayIt().until(x -> {
        try {
          latch.await();
          return Uni.createFrom().nullItem();
        }
        catch (InterruptedException ex) {
          return Uni.createFrom().failure(ex);
        }
      });
  }

  private static CountDownLatch createLatch() {
    return new CountDownLatch(1);
  }

  private static Stream<TeamScore> createTeamStatsItems() {
    return Stream.of(
      new TeamScore(0, 4),
      new TeamScore(1, 3),
      new TeamScore(2, 2),
      new TeamScore(3, 1),
      new TeamScore(4, 0)
    );
  }

  private static Stream<Iterable<Score>> createTopWinnerItems() {
    return Stream.of(
      List.of(new Score("Chewbacca", 5)),
      List.of(new Score("Darth Vader", 2)),
      List.of(new Score("Han Solo", 1)),
      List.of(new Score("Palpatine", 3))
    );
  }

  private class WebsocketTestClient {
    private final String stat;
    private final URI uri;
    private final BlockingQueue<String> messages;
    private final BasicWebSocketConnector connector;
    private final CountDownLatch latch;
    private Optional<WebSocketClientConnection> connection = Optional.empty();

    public WebsocketTestClient(String stat, URI uri, BlockingQueue<String> messages, BasicWebSocketConnector connector, CountDownLatch latch) {
      this.stat = stat;
      this.uri = uri;
      this.messages = messages;
      this.connector = connector;
      this.latch = latch;
    }

    public void connect() {
      close();

      this.connection = Optional.of(this.connector
        .baseUri(this.uri)
        .executionModel(ExecutionModel.NON_BLOCKING)
        .onOpen(c -> {
          Log.infof("Opening socket on connection %s for /stat/%s", c.id(), this.stat);
          this.messages.offer("CONNECT");
        })
        .onClose((c, closeReason) -> Log.infof("Closing socket on connection %s for /stats/%s: %s", c.id(), this.stat, closeReason))
        .onError((c, error) -> Log.errorf(error, "Socket on connection %s for /stat/%s encountered error", c.id(), this.stat))
        .onTextMessage((c, msg) -> {
          Log.infof("Got message on connection %s for /stats/%s: %s", c.id(), this.stat, msg);
          this.messages.offer(msg);
        })
        .connectAndAwait());

      waitForClientToStart();
    }

    public void close() {
      this.connection.ifPresent(WebSocketClientConnection::closeAndAwait);
    }

    protected void waitForClientToStart() {
      await()
        .atMost(Duration.ofMinutes(5))
        .until(() -> "CONNECT".equals(this.messages.poll()));

      this.latch.countDown();
    }
  }
}
