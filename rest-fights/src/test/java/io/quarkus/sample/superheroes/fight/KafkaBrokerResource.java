package io.quarkus.sample.superheroes.fight;

import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class KafkaBrokerResource implements QuarkusTestResourceLifecycleManager {
	private KafkaConsumer<String, Fight> fightConsumer;
	private final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));

	@Override
	public Map<String, String> start() {
		var objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		this.kafka.start();
		this.fightConsumer = new KafkaConsumer<>(consumerConfig(), new StringDeserializer(), new ObjectMapperDeserializer<>(Fight.class, objectMapper));

		return Map.of("kafka.bootstrap.servers", this.kafka.getBootstrapServers());
	}

	@Override
	public void stop() {
		this.fightConsumer.close();
		this.kafka.close();
	}

	private Properties consumerConfig() {
		var properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafka.getBootstrapServers());
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "fights");
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		return properties;
	}

	@Override
	public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(
			this.fightConsumer,
			new AnnotatedAndMatchesType(InjectKafkaConsumer.class, KafkaConsumer.class)
		);
	}
}
