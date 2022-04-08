package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.resteasy.reactive.client.impl.UniInvoker;

import io.quarkus.sample.superheroes.fight.config.FightConfig;

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
			.target(fightConfig.villain().clientBaseUrl())
			.path("api/villains/");
	}

	/**
	 * Gets a Villain from the Villain service wrapped with a recovery on a {@code 404} error. Also wrapped in a {@link CircuitBreaker}.
	 * @return The Villain
	 */
	@CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
	@CircuitBreakerName("findRandomVillain")
	CompletionStage<Villain> getRandomVillain() {
		// Want the 404 handling to be part of the circuit breaker
		// This means that the 404 responses aren't considered errors by the circuit breaker
		return this.villainClient.path("random")
			.request(MediaType.APPLICATION_JSON_TYPE)
			.rx(UniInvoker.class)
			.get(Villain.class)
			.onFailure(Is404Exception.IS_404).recoverWithNull()
			.subscribeAsCompletionStage();
	}

	/**
	 * Finds a random {@link Villain}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
	 * @return A random {@link Villain}
	 */
	public Uni<Villain> findRandomVillain() {
		// The CompletionState is important so that on retry the Uni re-subscribes to a new
		// CompletionStage rather than the original one (which has already completed)
		return Uni.createFrom().completionStage(this::getRandomVillain)
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}

  /**
   * Calls hello on the Villains service.
   * @return A "hello" from Villains
   */
  public String helloVillains() {
    return this.villainClient.path("hello")
      .request(MediaType.TEXT_PLAIN_TYPE)
      .get(String.class);
  }
}
