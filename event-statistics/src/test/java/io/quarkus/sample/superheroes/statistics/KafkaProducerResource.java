package io.quarkus.sample.superheroes.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import io.quarkus.kafka.client.serialization.ObjectMapperSerializer;
import io.quarkus.sample.superheroes.statistics.domain.Fight;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager}. Listens to the {@link DevServicesContext#ContextAware} event to bind a {@link KafkaProducer} to the {@code kafka.bootstrap.servers} address used by <a href="https://quarkus.io/guides/kafka-dev-services">Quarkus Dev Services for Kafka</a>.
 * <p>
 *   The {@link KafkaProducer} can then be injected into any tests via the {@link InjectKafkaProducer} annotation.
 * </p>
 * @see InjectKafkaProducer
 */
public class KafkaProducerResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
	private static final AtomicReference<Map<String, String>> DEV_SERVICES_PROPERTIES = new AtomicReference<>(new HashMap<>());
	private KafkaProducer<String, Fight> fightProducer;

	@Override
	public Map<String, String> start() {
		this.fightProducer = new KafkaProducer<>(producerConfig(), new StringSerializer(), new ObjectMapperSerializer<>());

		return Map.of();
	}

	@Override
	public void stop() {
		if (this.fightProducer != null) {
			this.fightProducer.close();
		}
	}

	private Map<String, Object> producerConfig() {
		return Map.of(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_SERVICES_PROPERTIES.get().get("kafka.bootstrap.servers")
		);
	}

	@Override
	public void inject(TestInjector testInjector) {
		testInjector.injectIntoFields(
			this.fightProducer,
			new AnnotatedAndMatchesType(InjectKafkaProducer.class, KafkaProducer.class)
		);
	}

	@Override
	public void setIntegrationTestContext(DevServicesContext context) {
		DEV_SERVICES_PROPERTIES.getAndSet(context.devServicesProperties());
	}
}
