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

import io.quarkus.logging.Log;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@QuarkusTest
class TeamStatsWebSocketTests {
	private static final BlockingQueue<String> MESSAGES = new LinkedBlockingQueue<>();

	@TestHTTPResource("/stats/team")
	URI uri;

	@InjectMock
	TeamStatsChannelHolder teamStatsChannelHolder;

	@Test
	public void runTest() throws DeploymentException, IOException, InterruptedException {
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

		when(this.teamStatsChannelHolder.getTeamStats()).thenReturn(delayedItemsMulti);

		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, this.uri)) {
			assertThat(MESSAGES.poll(10, TimeUnit.SECONDS))
				.isNotNull()
				.isEqualTo("CONNECT");

			// We're connected - trigger the Multi subscription
			latch.countDown();

			var expectedItems = createItems()
				.map(String::valueOf)
				.collect(Collectors.toList());

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofSeconds(30))
				.until(() -> MESSAGES.size() == expectedItems.size());

			Log.infof("Messages received by test: %s", MESSAGES);

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
			Log.infof("Got message: %s", msg);
			MESSAGES.add(msg);
		}

		@OnClose
		public void onClose(Session session) {
			MESSAGES.clear();
		}
	}
}
