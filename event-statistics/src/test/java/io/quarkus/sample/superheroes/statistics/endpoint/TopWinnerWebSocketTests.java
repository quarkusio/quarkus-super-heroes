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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

/**
 * Tests for the {@link TopWinnerWebSocket} class.
 * <p>
 *   These tests mock the {@link TopWinnerStatsChannelHolder#getWinners()} method to return pre-defined input and then set up a sample WebSocket client to listen to messages sent by the server. Each message received is placed into a {@link BlockingQueue} so that message content can be asserted once the expected number of messages have been received.
 * </p>
 */
@QuarkusTest
class TopWinnerWebSocketTests {
	@TestHTTPResource("/stats/winners")
	URI uri;

	@InjectMock
	TopWinnerStatsChannelHolder topWinnerStatsChannelHolder;

	@Inject
	ObjectMapper objectMapper;

	@Test
	public void topWinnerWebSocketTestScenario() throws DeploymentException, IOException, InterruptedException {
		// Set up the Queue to handle the messages
		var messages = new LinkedBlockingQueue<String>();

		// Set up a single consumer latch
		// It will wait for the client to connect and subscribe to the stream before emitting items
		var latch = new CountDownLatch(1);
		var delayedUni = Uni.createFrom().voidItem().onItem().delayIt()
			.until(x -> {
				try {
					latch.await();
					return Uni.createFrom().nullItem();
				}
				catch (InterruptedException ex) {
					return Uni.createFrom().failure(ex);
				}
			});

		// Delay the emission of the Multi until the client subscribes
		var delayedItemsMulti = Multi.createFrom().items(TopWinnerWebSocketTests::createItems)
			.onItem().call(scores -> Uni.createFrom().nullItem().onItem().delayIt().until(o -> delayedUni));

		// Mock TopWinnerStatsChannelHolder.getWinners() to return the delayed Multi
		when(this.topWinnerStatsChannelHolder.getWinners()).thenReturn(delayedItemsMulti);

		// Set up the client to connect to the socket
		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(new Client(messages), this.uri)) {
			// Make sure client connected
			assertThat(messages.poll(5, TimeUnit.MINUTES))
				.isNotNull()
				.isEqualTo("CONNECT");

			// Client has connected - trigger the Multi subscription
			latch.countDown();

			var expectedItems = createItems().collect(Collectors.toList());

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofMinutes(5))
				.until(() -> messages.size() == expectedItems.size());

			System.out.println("Messages received by test: " + messages);

			// Perform assertions that all expected messages were received
			expectedItems.stream()
				.map(Unchecked.function(this.objectMapper::writeValueAsString))
				.forEach(expectedMsg ->
					assertThat(messages.poll())
						.isNotNull()
						.isEqualTo(expectedMsg)
				);
		}
	}

	private static Stream<Iterable<Score>> createItems() {
		return Stream.of(
			List.of(new Score("Chewbacca", 5)),
			List.of(new Score("Darth Vader", 2)),
			List.of(new Score("Han Solo", 1)),
			List.of(new Score("Palpatine", 3))
		);
	}

	@ClientEndpoint
	private class Client {
		private final BlockingQueue<String> messages;

		private Client(BlockingQueue<String> messages) {
			this.messages = messages;
		}
		
		@OnOpen
		public void open(Session session) {
			this.messages.add("CONNECT");
		}

		@OnMessage
		void message(String msg) {
			this.messages.add(msg);
		}

		@OnClose
		public void onClose(Session session) {
			this.messages.clear();
		}
	}
}
