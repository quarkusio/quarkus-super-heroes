package io.quarkus.sample.superheroes.fight.client;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.logging.Log;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
	 * Finds a random {@link Hero}. The retry logic is applied to the result of the {@link CircuitBreaker}, meaning that retries that return failures could trigger the breaker to open.
	 * @return A random {@link Hero}
	 */
  @CircuitBreaker(requestVolumeThreshold = 8, failureRatio = 0.5, delay = 2, delayUnit = ChronoUnit.SECONDS)
	@CircuitBreakerName("findRandomHero")
  @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
  @WithSpan(kind = SpanKind.CLIENT, value = "HeroClient.findRandomHero")
	public Uni<Hero> findRandomHero() {
		// Want the 404 handling to be part of the circuit breaker
		// This means that the 404 responses aren't considered errors by the circuit breaker
    Log.debug("Making request to hero service to find random hero");
		return this.heroClient.findRandomHero()
			.onFailure(Is404Exception.IS_404).recoverWithNull();
	}
  
	/**
	 * Calls hello on the Heroes service.
	 * @return A "hello" from Heroes
	 */
  @WithSpan(kind = SpanKind.CLIENT, value = "HeroClient.helloHeroes")
	public Uni<String> helloHeroes() {
    Log.debug("Pinging hero service");
		return heroClient.hello();
	}
}
