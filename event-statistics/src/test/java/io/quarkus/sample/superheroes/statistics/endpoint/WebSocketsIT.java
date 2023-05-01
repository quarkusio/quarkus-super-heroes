package io.quarkus.sample.superheroes.statistics.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.sample.superheroes.statistics.domain.TeamScore;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.kafka.InjectKafkaCompanion;
import io.quarkus.test.kafka.KafkaCompanionResource;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.serde.avro.ReflectAvroDatumProvider;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import io.vertx.core.Vertx;

/**
 * Integration tests for the {@link TeamStatsWebSocket} and {@link TopWinnerWebSocket} WebSockets.
 * <p>
 *   These tests use the {@link KafkaCompanionResource} to create a {@link io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion}, injected via {@link InjectKafkaCompanion}.
 *   The test will publish messages to the Kafka topic while also creating WebSocket clients to listen to messages.
 *   Each received message is stored in a {@link BlockingQueue} so that the test can assert the correct messages were produced by the WebSocket.
 * </p>
 */
@QuarkusIntegrationTest
@QuarkusTestResource(value = KafkaCompanionResource.class, restrictToAnnotatedClass = true)
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

  @InjectKafkaCompanion
  KafkaCompanion companion;

  private static Vertx vertx;

  @BeforeAll
  public static void beforeAll() {
    OBJECT_MAPPER.setSerializationInclusion(Include.NON_EMPTY);
    OBJECT_MAPPER.registerModule(new ParameterNamesModule(Mode.PROPERTIES));
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Set Apicurio Avro
    vertx = Vertx.vertx();
    RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx));
  }

  @AfterAll
  static void afterAll() {
    Optional.ofNullable(vertx)
      .ifPresent(Vertx::close);
  }

  @BeforeEach
  public void beforeEach() {
    // Configure Avro Serde for Fight
    companion.setCommonClientConfig(Map.of(AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, ReflectAvroDatumProvider.class.getName()));
    Serde<Fight> serde = Serdes.serdeFrom(new AvroKafkaSerializer<>(), new AvroKafkaDeserializer<>());
    serde.configure(companion.getCommonClientConfig(), false);
    companion.registerSerde(io.quarkus.sample.superheroes.fight.schema.Fight.class, serde);
  }

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
      companion.produce(Fight.class)
        .fromRecords(sampleFights.stream()
          .map(fight -> new ProducerRecord<String, Fight>("fights", fight))
          .collect(Collectors.toList()));

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
    var teamScores = teamStatsMessages.stream()
      .filter(Objects::nonNull)
      .map(WebSocketsIT::createTeamScoreFromJsonString)
      .collect(Collectors.toCollection(LinkedList::new));

		assertThat(teamScores.poll())
			.isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(1, 0));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(1, 1));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(2, 1));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(2, 2));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(3, 2));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(3, 3));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(4, 3));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(4, 4));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(5, 4));

    assertThat(teamScores.poll())
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new TeamScore(5, 5));
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

  private static TeamScore createTeamScoreFromJsonString(String teamScoreJson) {
    return Unchecked.supplier(() -> OBJECT_MAPPER.readValue(teamScoreJson, TeamScore.class)).get();
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
