package io.quarkus.sample.superheroes.fight;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to inject one of the following into a test annotated with either {@link io.quarkus.test.junit.QuarkusTest @QuarkusTest} or {@link io.quarkus.test.junit.QuarkusIntegrationTest @QuarkusIntegrationTest}:
 * <p>
 *   <ul>
 *     <li>{@link org.wiremock.grpc.dsl.WireMockGrpcService WireMockGrpcService}</li>
 *   </ul>
 * </p>
 * <p>
 *   <pre>
 *     {@code
 * @InjectWireMock
 * WireMockGrpcService wireMockGrpcServer;
 *     }
 *   </pre>
 * </p>
 * @see LocationsWiremockGrpcServerResource
 */
@Target({ METHOD, CONSTRUCTOR, FIELD })
@Retention(RUNTIME)
@Documented
public @interface InjectGrpcWireMock {
}
