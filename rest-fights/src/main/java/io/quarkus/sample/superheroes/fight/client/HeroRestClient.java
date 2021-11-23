package io.quarkus.sample.superheroes.fight.client;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

/**
 * <a href="https://quarkus.io/guides/rest-client-reactive">Quarkus Reactive Rest Client</a> that talks to the Hero service.
 * <p>
 *   It is declared package-private so that the default client can be decorated by {@link HeroClient}. Consumers should use {@link HeroClient}.
 * </p>
 */
@Path("/api/heroes")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "hero-client")
interface HeroRestClient {
	/**
	 * HTTP <code>GET</code> call to {@code /api/heroes/random} on the Heros service
	 * @return A {@link Hero}
	 * @see HeroClient#findRandomHero()
	 */
	@GET
	@Path("/random")
	Uni<Hero> findRandomHero();
}
