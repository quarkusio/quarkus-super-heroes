package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.InjectKafkaProducer;
import io.quarkus.sample.superheroes.statistics.KafkaProducerResource;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.unchecked.Unchecked;

/**
 * Integration tests for the {@link TeamStatsWebSocket} and {@link TopWinnerWebSocket} WebSockets.
 * <p>
 *   These tests use the {@link KafkaProducerResource} to create a {@link KafkaProducer}, injected via {@link InjectKafkaProducer}. The test will publish messages to the Kafka topic while also creating WebSocket clients to listen to messages. Each received message is stored in a {@link BlockingQueue} so that the test can assert the correct messages were produced by the WebSocket.
 * </p>
 */
@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaProducerResource.class, restrictToAnnotatedClass = true)
public class WebSocketsIT {
	private static final String HERO_NAME = "Chewbacca";
	private static final String HERO_TEAM_NAME = "heroes";
	private static final String VILLAIN_TEAM_NAME = "villains";
	private static final String VILLAIN_NAME = "Darth Vader";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@TestHTTPResource("/stats/team")
	URI teamStatsUri;

	@TestHTTPResource("/stats/winners")
	URI topWinnersUri;

	@InjectKafkaProducer
	KafkaProducer<String, Fight> fightsProducer;

	@Test
	public void testScenarios() throws DeploymentException, IOException {
		// Set up the Queues to handle the messages
		var teamStatsMessages = new LinkedBlockingQueue<String>();
		var topWinnerMessages = new LinkedBlockingQueue<String>();

		// Set up the clients to connect to the sockets
		try (
			var teamStatsSession = ContainerProvider.getWebSocketContainer().connectToServer(new EndpointTestClient(teamStatsMessages), this.teamStatsUri);
			var topWinnersSession = ContainerProvider.getWebSocketContainer().connectToServer(new EndpointTestClient(topWinnerMessages), this.topWinnersUri)
		) {
			// Make sure clients connected
			// Wait for each client to emit a CONNECT message
			waitForClientsToStart(teamStatsMessages, topWinnerMessages);

			// Create 10 fights, split between heroes and villains winning
			var sampleFights = createSampleFights();

			// Publish messages to the Kafka topic
			sampleFights.stream()
				.map(fight -> new ProducerRecord<String, Fight>("fights", fight))
				.forEach(this.fightsProducer::send);

			// Wait for our messages to appear in the queue
			await()
				.atMost(Duration.ofMinutes(5))
				.until(() -> (teamStatsMessages.size() == sampleFights.size()) && (topWinnerMessages.size() == sampleFights.size()));

			validateTeamStats(teamStatsMessages);
			validateTopWinnerStats(topWinnerMessages);
		}
	}

	private static void validateTeamStats(BlockingQueue<String> teamStatsMessages) {
		System.out.println("Team Stats Messages received by test: " + teamStatsMessages);

		// Perform assertions that all expected teamStatsMessages were received
		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(1, 0)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(1, 1)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(2, 1)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(2, 2)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(3, 2)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(3, 3)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(4, 3)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(4, 4)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(5, 4)));

		assertThat(teamStatsMessages.poll())
			.isNotNull()
      .isEqualTo(createTeamScoreJsonString(new TeamScore(5, 5)));
	}

	private static void validateTopWinnerStats(BlockingQueue<String> topWinnerMessages) {
		System.out.println("Top Winner Messages received by test: " + topWinnerMessages);

		// Perform assertions that all expected topWinnerMessages were received
		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s]", createScoreJsonString(HERO_NAME, 1));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 1), createScoreJsonString(VILLAIN_NAME, 1));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 2), createScoreJsonString(VILLAIN_NAME, 1));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 2), createScoreJsonString(VILLAIN_NAME, 2));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 3), createScoreJsonString(VILLAIN_NAME, 2));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 3), createScoreJsonString(VILLAIN_NAME, 3));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 4), createScoreJsonString(VILLAIN_NAME, 3));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 4), createScoreJsonString(VILLAIN_NAME, 4));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 5), createScoreJsonString(VILLAIN_NAME, 4));

		assertThat(topWinnerMessages.poll())
			.isNotNull()
			.isEqualTo("[%s,%s]", createScoreJsonString(HERO_NAME, 5), createScoreJsonString(VILLAIN_NAME, 5));
	}

	private static void waitForClientsToStart(BlockingQueue<String> teamStatsMessages, BlockingQueue<String> topWinnerMessages) {
		await()
			.atMost(Duration.ofMinutes(5))
			.pollInterval(Duration.ofSeconds(10))
			.until(() -> "CONNECT".equals(teamStatsMessages.poll()) && "CONNECT".equals(topWinnerMessages.poll()));
	}

  private static String createTeamScoreJsonString(TeamScore teamScore) {
    return Unchecked.supplier(() -> OBJECT_MAPPER.writeValueAsString(teamScore)).get();
  }

	private static String createScoreJsonString(String name, int score) {
		return String.format("{\"name\":\"%s\",\"score\":%d}", name, score);
	}

	private static List<Fight> createSampleFights() {
		return IntStream.range(0, 10)
			.mapToObj(i -> {
				var heroName = HERO_NAME;
				var villainName = VILLAIN_NAME;
				var fight = Fight.newBuilder()
          .setFightDate(Instant.now())
          .setWinnerLevel(2)
          .setLoserLevel(1);

				if (i % 2 == 0) {
					fight = fight.setWinnerTeam(HERO_TEAM_NAME)
						.setLoserTeam(VILLAIN_TEAM_NAME)
						.setWinnerName(heroName)
						.setLoserName(villainName);
				}
				else {
					fight = fight.setWinnerTeam(VILLAIN_TEAM_NAME)
						.setLoserTeam(HERO_TEAM_NAME)
						.setWinnerName(villainName)
						.setLoserName(heroName);
				}

				return fight.build();
			}).collect(Collectors.toList());
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
