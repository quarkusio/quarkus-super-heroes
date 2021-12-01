package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.statistics.InjectKafkaProducer;
import io.quarkus.sample.superheroes.statistics.KafkaProducerResource;
import io.quarkus.sample.superheroes.statistics.domain.Fight;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Integration tests for the {@link TeamStatsWebSocket} WebSocket.
 * <p>
 *   These tests use the {@link KafkaProducerResource} to create a {@link KafkaProducer}, injected via {@link InjectKafkaProducer}. The test will publish messages to the Kafka topic while also creating a WebSocket client to listen to messages. Each received message is stored in a {@link BlockingQueue} so that the test can assert the correct messages were produced by the WebSocket.
 * </p>
 */
@QuarkusIntegrationTest
@QuarkusTestResource(KafkaProducerResource.class)
public class TeamStatsWebSocketIT {
	private static final String HERO_NAME = "Chewbacca";
	private static final String HERO_TEAM_NAME = "heroes";
	private static final String VILLAIN_TEAM_NAME = "villains";
	private static final String VILLAIN_NAME = "Darth Vader";
	private static final BlockingQueue<String> MESSAGES = new LinkedBlockingQueue<>();

	@TestHTTPResource("/stats/team")
	URI uri;

	@InjectKafkaProducer
	KafkaProducer<String, Fight> fightsProducer;

	@Test
	public void teamStatsWebSocketTestScenario() throws DeploymentException, IOException, InterruptedException {
		// Set up the client to connect to the socket
		try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, this.uri)) {
			// Make sure client connected
			assertThat(MESSAGES.poll(2, TimeUnit.MINUTES))
				.isNotNull()
				.isEqualTo("CONNECT");

			// Create 10 fights, split between heroes and villains winning
			var sampleFights = createSampleFights().collect(Collectors.toList());
			sampleFights.stream()
				.map(fight -> new ProducerRecord<String, Fight>("fights", fight))
				.forEach(this.fightsProducer::send);

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofMinutes(2))
				.until(() -> MESSAGES.size() == sampleFights.size());

			System.out.println("Messages received by test: " + MESSAGES);

			// Perform assertions that all expected messages were received
			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 1/1));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 1/2));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 2/3));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 2/4));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 3/5));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 3/6));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 4/7));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 4/8));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 5/9));

			assertThat(MESSAGES.poll())
				.isNotNull()
				.isEqualTo(String.valueOf((double) 5/10));
		}
	}

	private Stream<Fight> createSampleFights() {
		return IntStream.range(0, 10)
			.mapToObj(i -> {
				var heroName = HERO_NAME;
				var villainName = VILLAIN_NAME;
				var fight = Fight.builder();

				if (i % 2 == 0) {
					fight = fight.winnerTeam(HERO_TEAM_NAME)
						.loserTeam(VILLAIN_TEAM_NAME)
						.winnerName(heroName)
						.loserName(villainName);
				}
				else {
					fight = fight.winnerTeam(VILLAIN_TEAM_NAME)
						.loserTeam(HERO_TEAM_NAME)
						.winnerName(villainName)
						.loserName(heroName);
				}

				return fight.build();
			});
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
