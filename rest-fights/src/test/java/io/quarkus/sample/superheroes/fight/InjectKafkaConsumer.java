package io.quarkus.sample.superheroes.fight;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to inject a {@link org.apache.kafka.clients.consumer.KafkaConsumer KafkaConsumer} into a test annotated with either {@link io.quarkus.test.junit.QuarkusTest @QuarkusTest} or {@link io.quarkus.test.junit.QuarkusIntegrationTest @QuarkusIntegrationTest}.
 * <p>
 *   <pre>
 *     {@code
 * @InjectKafkaConsumer
 * KafkaConsumer<String, Fight> fightsConsumer;
 *     }
 *   </pre>
 * </p>
 * @see KafkaBrokerResource
 */
@Target({ METHOD, CONSTRUCTOR, FIELD })
@Retention(RUNTIME)
@Documented
public @interface InjectKafkaConsumer {
}
