package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

@QuarkusTest
class TopWinnerWebSocketTests {
	private static final BlockingQueue<String> MESSAGES = new LinkedBlockingQueue<>();

	@TestHTTPResource("/stats/winners")
	URI uri;

	@InjectMock
	WinnerStatsChannelHolder winnerStatsChannelHolder;

	@Inject
	ObjectMapper objectMapper;

	@Test
	public void runTest() throws DeploymentException, IOException, InterruptedException {
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

		Mockito.when(this.winnerStatsChannelHolder.getWinners())
			.thenReturn(delayedItemsMulti);

		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, this.uri)) {
			assertThat(MESSAGES.poll(10, TimeUnit.SECONDS))
				.isNotNull()
				.isEqualTo("CONNECT");

			// We're connected - trigger the Multi subscription
			latch.countDown();

			var expectedItems = createItems().collect(Collectors.toList());

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofSeconds(30))
				.until(() -> MESSAGES.size() == expectedItems.size());

			Log.infof("Messages received by test: %s", MESSAGES);

			expectedItems.stream()
				.map(Unchecked.function(this.objectMapper::writeValueAsString))
				.forEach(expectedMsg ->
					assertThat(MESSAGES.poll())
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
	public static class Client {
		@OnOpen
		public void open(Session session) {
			MESSAGES.add("CONNECT");
		}

		@OnMessage
		void message(String msg) {
			Log.infof("Got message: %s", msg);
			MESSAGES.add(msg);
		}
	}
}
