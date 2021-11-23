package io.quarkus.sample.superheroes.fight.client;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.impl.UniInvoker;

import io.quarkus.sample.superheroes.fight.config.FightConfig;

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
			.path("api/villains/random");
	}

	/**
	 * Finds a random {@link Villain}
	 * @return A random {@link Villain}
	 */
	public Uni<Villain> findRandomVillain() {
		return this.villainClient
			.request(MediaType.APPLICATION_JSON_TYPE)
			.rx(UniInvoker.class)
			.get(Villain.class)
			.onFailure(Is404Exception.IS_404).recoverWithNull()
			.onFailure().retry().withBackOff(Duration.ofMillis(200)).atMost(3);
	}
}
