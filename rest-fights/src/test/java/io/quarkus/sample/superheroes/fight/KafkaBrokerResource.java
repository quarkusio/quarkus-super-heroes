package io.quarkus.sample.superheroes.fight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager} binding the {@code kafka.bootstrap.servers}, and exposing it to tests that want to inject it via {@link InjectKafkaConsumer}.
 * @see InjectKafkaConsumer
 */
public class KafkaBrokerResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
	private static final AtomicReference<Map<String, String>> DEV_SERVICES_PROPERTIES = new AtomicReference<>(new HashMap<>());
	private KafkaConsumer<String, Fight> fightConsumer;

	@Override
	public Map<String, String> start() {
		var objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		this.fightConsumer = new KafkaConsumer<>(consumerConfig(), new StringDeserializer(), new ObjectMapperDeserializer<>(Fight.class, objectMapper));
		this.fightConsumer.subscribe(List.of("fights"));

		return Map.of();
	}

	@Override
	public void stop() {
		if (this.fightConsumer != null) {
			this.fightConsumer.unsubscribe();
			this.fightConsumer.close();
		}
	}

	private Properties consumerConfig() {
		var properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_SERVICES_PROPERTIES.get().get("kafka.bootstrap.servers"));
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

	@Override
	public void setIntegrationTestContext(DevServicesContext context) {
		DEV_SERVICES_PROPERTIES.getAndSet(context.devServicesProperties());
	}
}
