package io.quarkus.sample.superheroes.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import io.quarkus.sample.superheroes.fight.schema.Fight;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaSerializer;
import io.apicurio.registry.serde.avro.ReflectAvroDatumProvider;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.vertx.core.Vertx;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager}. Listens to the {@link DevServicesContext#ContextAware} event to bind a {@link KafkaProducer} to the {@code kafka.bootstrap.servers} address used by <a href="https://quarkus.io/guides/kafka-dev-services">Quarkus Dev Services for Kafka</a>.
 * <p>
 *   The {@link KafkaProducer} can then be injected into any tests via the {@link InjectKafkaProducer} annotation.
 * </p>
 * @see InjectKafkaProducer
 */
public class KafkaProducerResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
	private static final AtomicReference<Map<String, String>> DEV_SERVICES_PROPERTIES = new AtomicReference<>(new HashMap<>());
  private Vertx vertx;
	private KafkaProducer<String, Fight> fightProducer;

	@Override
	public Map<String, String> start() {
    this.vertx = Vertx.vertx();
    RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx));

		this.fightProducer = new KafkaProducer<>(producerConfig());
		return Map.of();
	}

	@Override
	public void stop() {
    Optional.ofNullable(this.fightProducer)
      .ifPresent(KafkaProducer::close);

    Optional.ofNullable(this.vertx)
      .ifPresent(Vertx::close);
	}

	private Map<String, Object> producerConfig() {
		return Map.of(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_SERVICES_PROPERTIES.get().get("kafka.bootstrap.servers"),
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName(),
      SerdeConfig.AUTO_REGISTER_ARTIFACT, "true",
      SerdeConfig.REGISTRY_URL, DEV_SERVICES_PROPERTIES.get().get("mp.messaging.connector.smallrye-kafka.apicurio.registry.url"),
      AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, ReflectAvroDatumProvider.class.getName()
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
