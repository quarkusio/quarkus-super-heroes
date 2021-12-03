package io.quarkus.sample.superheroes.statistics.endpoint;

/**
 * Integration tests for the {@link TopWinnerWebSocket} WebSocket.
 * <p>
 *   These tests use the {@link io.quarkus.sample.superheroes.statistics.KafkaProducerResource} to create a {@link KafkaProducer}, injected via {@link io.quarkus.sample.superheroes.statistics.InjectKafkaProducer}. The test will publish messages to the Kafka topic while also creating a WebSocket client to listen to messages. Each received message is stored in a {@link java.util.concurrent.BlockingQueue} so that the test can assert the correct messages were produced by the WebSocket.
 * </p>
 */
//@QuarkusIntegrationTest
//@QuarkusTestResource(KafkaProducerResource.class)
public class TopWinnerWebSocketIT {
//	private static final String HERO_NAME = "Chewbacca";
//	private static final String HERO_TEAM_NAME = "heroes";
//	private static final String VILLAIN_TEAM_NAME = "villains";
//	private static final String VILLAIN_NAME = "Darth Vader";
//
//	@TestHTTPResource("/stats/winners")
//	URI uri;
//
//	@InjectKafkaProducer
//	KafkaProducer<String, Fight> fightsProducer;
//
//	@Test
//	public void topWinnerWebSocketTestScenario() throws DeploymentException, IOException, InterruptedException {
//		// Set up the Queue to handle the messages
//		var messages = new LinkedBlockingQueue<String>();
//
//		// Set up the client to connect to the socket
//		try (var session = ContainerProvider.getWebSocketContainer().connectToServer(new EndpointTestClient(messages), this.uri)) {
//			// Make sure client connected
//			assertThat(messages.poll(5, TimeUnit.MINUTES))
//				.isNotNull()
//				.isEqualTo("CONNECT");
//
//			// Create 10 fights, split between heroes and villains winning
//			var sampleFights = createSampleFights();
//			sampleFights.stream()
//				.map(fight -> new ProducerRecord<String, Fight>("fights", fight))
//				.forEach(this.fightsProducer::send);
//
//			// Wait for our messages to appear in the queue
//			await()
//				.atMost(Duration.ofMinutes(5))
//				.until(() -> messages.size() == sampleFights.size());
//
//			System.out.println("Messages received by test: " + messages);
//
//			// Perform assertions that all expected messages were received
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s]", createJsonString(HERO_NAME, 1));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 1), createJsonString(VILLAIN_NAME, 1));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 2), createJsonString(VILLAIN_NAME, 1));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 2), createJsonString(VILLAIN_NAME, 2));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 3), createJsonString(VILLAIN_NAME, 2));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 3), createJsonString(VILLAIN_NAME, 3));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 4), createJsonString(VILLAIN_NAME, 3));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 4), createJsonString(VILLAIN_NAME, 4));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 5), createJsonString(VILLAIN_NAME, 4));
//
//			assertThat(messages.poll())
//				.isNotNull()
//				.isEqualTo("[%s,%s]", createJsonString(HERO_NAME, 5), createJsonString(VILLAIN_NAME, 5));
//		}
//	}
//
//	private static String createJsonString(String name, int score) {
//		return String.format("{\"name\":\"%s\",\"score\":%d}", name, score);
//	}
//
//	private static List<Fight> createSampleFights() {
//		return IntStream.range(0, 10)
//			.mapToObj(i -> {
//				var heroName = HERO_NAME;
//				var villainName = VILLAIN_NAME;
//				var fight = Fight.builder();
//
//				if (i % 2 == 0) {
//					fight = fight.winnerTeam(HERO_TEAM_NAME)
//						.loserTeam(VILLAIN_TEAM_NAME)
//						.winnerName(heroName)
//						.loserName(villainName);
//				}
//				else {
//					fight = fight.winnerTeam(VILLAIN_TEAM_NAME)
//						.loserTeam(HERO_TEAM_NAME)
//						.winnerName(villainName)
//						.loserName(heroName);
//				}
//
//				return fight.build();
//			}).collect(Collectors.toList());
//	}
//
//	@ClientEndpoint
//	private class EndpointTestClient {
//		private final Logger logger = Logger.getLogger(EndpointTestClient.class);
//		private final BlockingQueue<String> messages;
//
//		private EndpointTestClient(BlockingQueue<String> messages) {
//			this.messages = messages;
//		}
//
//		@OnOpen
//		public void open() {
//			this.logger.info("Opening socket");
//			this.messages.offer("CONNECT");
//		}
//
//		@OnMessage
//		public void message(String msg) {
//			this.logger.infof("Got message: %s", msg);
//			this.messages.offer(msg);
//		}
//
//		@OnClose
//		public void close(CloseReason closeReason) {
//			this.logger.infof("Closing socket: %s", closeReason);
//		}
//
//		@OnError
//		public void error(Throwable error) {
//			this.logger.errorf(error, "Socket encountered error");
//		}
//	}
}
