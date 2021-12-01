package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Tests for the {@link TeamStatsWebSocket} class.
 * <p>
 *   These tests mock the {@link TeamStatsChannelHolder#getTeamStats()} method to return pre-defined input and then set up a sample WebSocket client to listen to messages sent by the server. Each message received is placed into a {@link BlockingQueue} so that message content can be asserted once the expected number of messages have been received.
 * </p>
 */
@QuarkusTest
class TeamStatsWebSocketTests {
	private static final BlockingQueue<String> MESSAGES = new LinkedBlockingQueue<>();

	@TestHTTPResource("/stats/team")
	URI uri;

	@InjectMock
	TeamStatsChannelHolder teamStatsChannelHolder;

	@Test
	public void teamStatsWebSocketTestScenario() throws DeploymentException, IOException, InterruptedException {
		// Set up a single consumer latch
		// It will wait for the client to connect and subscribe to the stream before emitting items
		var latch = new CountDownLatch(1);
		var delayedUni = Uni.createFrom().voidItem()
			.onItem().delayIt().until(x -> {
				try {
					latch.await();
					return Uni.createFrom().nullItem();
				}
				catch (InterruptedException ex) {
					return Uni.createFrom().failure(ex);
				}
			});

		// Delay the emission of the Multi until the client subscribes
		var delayedItemsMulti = Multi.createFrom().items(TeamStatsWebSocketTests::createItems)
			.onItem().call(items -> Uni.createFrom().nullItem().onItem().delayIt().until(o -> delayedUni));

		// Mock TeamStatsChannelHolder.getTeamStats() to return the delayed Multi
		when(this.teamStatsChannelHolder.getTeamStats()).thenReturn(delayedItemsMulti);

		// Set up the client to connect to the socket
		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, this.uri)) {
			// Make sure client connected
			assertThat(MESSAGES.poll(2, TimeUnit.MINUTES))
				.isNotNull()
				.isEqualTo("CONNECT");

			// Client has connected - trigger the Multi subscription
			latch.countDown();

			var expectedItems = createItems()
				.map(String::valueOf)
				.collect(Collectors.toList());

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofMinutes(2))
				.until(() -> MESSAGES.size() == expectedItems.size());

			System.out.println("Messages received by test: " + MESSAGES);

			// Perform assertions that all expected messages were received
			assertThat(MESSAGES)
				.containsExactlyElementsOf(expectedItems);
		}
	}

	private static Stream<Double> createItems() {
		return DoubleStream.of(0.0, 0.25, 0.5, 0.75, 1.0).boxed();
	}

	@ClientEndpoint
	public static class Client {
		@OnOpen
		public void open(Session session) {
			MESSAGES.add("CONNECT");
		}

		@OnMessage
		void message(String msg) {
			MESSAGES.add(msg);
		}

		@OnClose
		public void onClose(Session session) {
			MESSAGES.clear();
		}
	}
}
