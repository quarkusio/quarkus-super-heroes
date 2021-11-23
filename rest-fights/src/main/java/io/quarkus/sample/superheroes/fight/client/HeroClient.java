package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.inject.RestClient;

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
	 * Finds a random {@link Hero}
	 * @return A random {@link Hero}
	 */
	public Uni<Hero> findRandomHero() {
		return this.heroClient.findRandomHero()
			.onFailure(Is404Exception.IS_404).recoverWithNull()
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}
}
