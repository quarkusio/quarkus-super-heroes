package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

/**
 * Tests for the {@link TopWinnerWebSocket} and {@link TeamStatsWebSocket} classes.
 * <p>
 *   These tests mock the {@link TopWinnerStatsChannelHolder#getWinners()} and {@link TeamStatsChannelHolder#getTeamStats()} methods to return pre-defined input and then set up a sample WebSocket client to listen to messages sent by the server. Each message received is placed into a {@link java.util.concurrent.BlockingQueue} so that message content can be asserted once the expected number of messages have been received.
 * </p>
 */
@QuarkusTest
public class WebSocketsTests {
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

	@Test
	public void testScenarios() throws DeploymentException, IOException {
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

		// Set up the clients to connect to the sockets
		try (
			var teamStatsSession = ContainerProvider.getWebSocketContainer().connectToServer(new EndpointTestClient(teamStatsMessages), this.teamStatsUri);
			var topWinnersSession = ContainerProvider.getWebSocketContainer().connectToServer(new EndpointTestClient(topWinnerMessages), this.topWinnersUri)
		) {
			// Make sure clients connected
			// Wait for each client to emit a CONNECT message
			waitForClientsToStart(teamStatsMessages, teamStatsLatch, topWinnerMessages, topWinnersLatch);

			var expectedTeamStats = createTeamStatsItems().collect(Collectors.toList());
			var expectedTopWinners = createTopWinnerItems().collect(Collectors.toList());

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofMinutes(5))
				.pollInterval(Duration.ofSeconds(10))
				.until(() -> (teamStatsMessages.size() == expectedTeamStats.size()) && (topWinnerMessages.size() == expectedTopWinners.size()));

			validateTeamStats(teamStatsMessages, expectedTeamStats);
			validateTopWinnerStats(topWinnerMessages, expectedTopWinners);
		}
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

	private static void waitForClientsToStart(BlockingQueue<String> teamStatsMessages, CountDownLatch teamStatsLatch, BlockingQueue<String> topWinnerMessages, CountDownLatch topWinnersLatch) {
		await()
			.atMost(Duration.ofMinutes(5))
			.pollInterval(Duration.ofSeconds(10))
			.until(() -> "CONNECT".equals(teamStatsMessages.poll()) && "CONNECT".equals(topWinnerMessages.poll()));

		teamStatsLatch.countDown();
		topWinnersLatch.countDown();
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

	@ClientEndpoint
	private class EndpointTestClient {
		private final Logger logger = Logger.getLogger(EndpointTestClient.class);
		private final BlockingQueue<String> messages;

		private EndpointTestClient(BlockingQueue<String> messages) {
			this.messages = messages;
		}

		@OnOpen
		public void open() {
			this.logger.info("Opening socket");
			this.messages.offer("CONNECT");
		}

		@OnMessage
		public void message(String msg) {
			this.logger.infof("Got message: %s", msg);
			this.messages.offer(msg);
		}

		@OnClose
		public void close(CloseReason closeReason) {
			this.logger.infof("Closing socket: %s", closeReason);
		}

		@OnError
		public void error(Throwable error) {
			this.logger.errorf(error, "Socket encountered error");
		}
	}
}
