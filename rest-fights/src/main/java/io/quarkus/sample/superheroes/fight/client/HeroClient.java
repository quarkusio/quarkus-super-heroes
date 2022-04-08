package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.mutiny.Uni;

/**
 * Bean to be used for interacting with the Hero service
 */
@ApplicationScoped
public class HeroClient {
	private final HeroRestClient heroClient;

	public HeroClient(@RestClient HeroRestClient heroClient) {
		this.heroClient = heroClient;
	}

	/**
	 * Gets a Hero from the Hero service wrapped with a recovery on a {@code 404} error. Also wrapped in a {@link CircuitBreaker}.
	 * @return The Hero
	 */
	@CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
	@CircuitBreakerName("findRandomHero")
	CompletionStage<Hero> getRandomHero() {
		// Want the 404 handling to be part of the circuit breaker
		// This means that the 404 responses aren't considered errors by the circuit breaker
		return this.heroClient.findRandomHero()
			.onFailure(Is404Exception.IS_404).recoverWithNull()
			.subscribeAsCompletionStage();
	}

	/**
	 * Finds a random {@link Hero}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
	 * @return A random {@link Hero}
	 */
	public Uni<Hero> findRandomHero() {
		// The CompletionState is important so that on retry the Uni re-subscribes to a new
		// CompletionStage rather than the original one (which has already completed)
		return Uni.createFrom().completionStage(this::getRandomHero)
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}
  
	/**
	 * Calls hello on the Heroes service.
	 * @return A "hello" from Heroes
	 */
	public String helloHeroes() {
		return heroClient.hello();
	}
}
