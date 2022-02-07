package io.quarkus.sample.superheroes.fight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager.TestInjector.AnnotatedAndMatchesType;

import io.apicurio.registry.rest.client.RegistryClientFactory;
import io.apicurio.registry.serde.SerdeConfig;
import io.apicurio.registry.serde.avro.AvroKafkaDeserializer;
import io.apicurio.registry.serde.avro.AvroKafkaSerdeConfig;
import io.apicurio.registry.serde.avro.ReflectAvroDatumProvider;
import io.apicurio.rest.client.VertxHttpClientProvider;
import io.vertx.core.Vertx;

/**
 * Quarkus {@link QuarkusTestResourceLifecycleManager}. Listens to the {@link DevServicesContext#ContextAware} event to bind a {@link KafkaConsumer} to the {@code kafka.bootstrap.servers} address used by <a href="https://quarkus.io/guides/kafka-dev-services">Quarkus Dev Services for Kafka</a>.
 * <p>
 *   The {@link KafkaConsumer} can then be injected into any tests via the {@link InjectKafkaConsumer} annotation.
 * </p>
 * @see InjectKafkaConsumer
 */
public class KafkaConsumerResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
  private static final AtomicReference<Map<String, String>> DEV_SERVICES_PROPERTIES = new AtomicReference<>(new HashMap<>());
  private Vertx vertx;
  private KafkaConsumer<String, io.quarkus.sample.superheroes.fight.schema.Fight> fightConsumer;

  @Override
  public Map<String, String> start() {
    this.vertx = Vertx.vertx();
    RegistryClientFactory.setProvider(new VertxHttpClientProvider(vertx));

    this.fightConsumer = new KafkaConsumer<>(consumerConfig());
    this.fightConsumer.subscribe(List.of("fights"));

    return Map.of();
  }

  @Override
  public void stop() {
    if (this.fightConsumer != null) {
      this.fightConsumer.unsubscribe();
      this.fightConsumer.close();
    }

    Optional.ofNullable(this.vertx)
      .ifPresent(Vertx::close);
  }

  private Map<String, Object> consumerConfig() {
    return Map.of(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, DEV_SERVICES_PROPERTIES.get().get("kafka.bootstrap.servers"),
      ConsumerConfig.GROUP_ID_CONFIG, "fights",
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName(),
      SerdeConfig.AUTO_REGISTER_ARTIFACT, "true",
      SerdeConfig.REGISTRY_URL, DEV_SERVICES_PROPERTIES.get().get("mp.messaging.connector.smallrye-kafka.apicurio.registry.url"),
      AvroKafkaSerdeConfig.AVRO_DATUM_PROVIDER, ReflectAvroDatumProvider.class.getName()
    );
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
