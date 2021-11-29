package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.statistics.domain.Score;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

import io.smallrye.reactive.messaging.connectors.InMemoryConnector;

@QuarkusTest
class TopWinnerWebSocketTests {
	private static final String CHANNEL_NAME = "winner-stats";
	private static final BlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

	@TestHTTPResource("/stats/winners")
	URI uri;

	@Inject
	@Any
	InMemoryConnector inMemoryConnector;

	@Test
	public void runTest() throws DeploymentException, IOException, InterruptedException {
		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, this.uri)) {
			assertThat(MESSAGES.poll(10, TimeUnit.SECONDS))
				.isNotNull()
				.isEqualTo("CONNECT");

			// Now let's inject some stats
			List.of(
				new Score("Chewbacca", 5),
				new Score("Darth Vader", 2),
				new Score("Han Solo", 1),
				new Score("Palpatine", 3)
			).forEach(this.inMemoryConnector.source(CHANNEL_NAME)::send);
			this.inMemoryConnector.source(CHANNEL_NAME).complete();
		}
	}

	@ClientEndpoint
	public static class Client {
		@OnOpen
		public void open(Session session) {
			MESSAGES.add("CONNECT");
			// Send a message to indicate that we are ready,
			// as the message handler may not be registered immediately after this callback.
			session.getAsyncRemote().sendText("_ready_");
		}

		@OnMessage
		void message(String msg) {
			System.out.println("Got message: " + msg);
			MESSAGES.add(msg);
		}
	}
}
