package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class HeroClient {
	private final HeroRestClient heroClient;

	public HeroClient(@RestClient HeroRestClient heroClient) {
		this.heroClient = heroClient;
	}

	public Uni<Hero> findRandomHero() {
		return this.heroClient.findRandomHero()
			.onFailure(Is404Exception.IS_404).recoverWithUni(() -> Uni.createFrom().nullItem())
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}
}
