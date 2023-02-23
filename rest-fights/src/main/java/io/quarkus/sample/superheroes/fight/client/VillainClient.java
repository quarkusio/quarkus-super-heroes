package io.quarkus.sample.superheroes.fight.client;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.ext.DefaultClientHeadersFactoryImpl;
import org.jboss.resteasy.reactive.client.impl.UniInvoker;

import io.quarkus.logging.Log;
import io.quarkus.rest.client.reactive.runtime.MicroProfileRestClientRequestFilter;
import io.quarkus.sample.superheroes.fight.config.FightConfig;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;

/**
 * Bean to be used for interacting with the Villain service.
 * <p>
 *   Uses the <a href="https://docs.oracle.com/javaee/7/tutorial/jaxrs-client001.htm">JAX-RS Rest Client</a> with the <a href="https://quarkus.io/guides/resteasy-reactive#resteasy-reactive-client">RESTEasy Reactive client</a>.
 * </p>
 */
@ApplicationScoped
public class VillainClient {
  private final WebTarget villainClient;

  public VillainClient(FightConfig fightConfig) {
    this.villainClient = ClientBuilder.newClient()
      .register(new MicroProfileRestClientRequestFilter(new DefaultClientHeadersFactoryImpl()))
      .target(fightConfig.villain().clientBaseUrl())
      .path("api/villains/");
  }

  /**
   * Finds a random {@link Villain}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
   * @return A random {@link Villain}
   */
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
  @CircuitBreakerName("findRandomVillain")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @WithSpan(kind = SpanKind.CLIENT, value = "VillainClient.findRandomVillain")
  public Uni<Villain> findRandomVillain() {
    // Want the 404 handling to be part of the circuit breaker
    // This means that the 404 responses aren't considered errors by the circuit breaker
    var target = this.villainClient.path("random");
    Log.debugf("Going to make request to %s", target.getUri());

    return target
      .request(MediaType.APPLICATION_JSON_TYPE)
      .rx(UniInvoker.class)
      .get(Villain.class)
      .invoke(villain -> Log.debugf("Got villain back from %s: %s", target.getUri(), villain))
      .onFailure(Is404Exception.IS_404).recoverWithNull();
  }

  /**
   * Calls hello on the Villains service.
   * @return A "hello" from Villains
   */
  @WithSpan(kind = SpanKind.CLIENT, value = "VillainClient.helloVillains")
  public Uni<String> helloVillains() {
    var target =this.villainClient.path("hello");
    Log.debugf("Going to make request to %s", target.getUri());

    return target
      .request(MediaType.TEXT_PLAIN_TYPE)
      .rx(UniInvoker.class)
      .get(String.class)
      .invoke(hello -> Log.debugf("Got response back from %s: %s", target.getUri(), hello));
  }
}
