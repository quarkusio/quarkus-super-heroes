package io.quarkus.sample.superheroes.fight.client;

import static jakarta.ws.rs.core.MediaType.*;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

/**
 * <a href="https://quarkus.io/guides/rest-client-reactive">Quarkus Reactive Rest Client</a> that talks to the Hero service.
 * <p>
 *   It is declared package-private so that the default client can be decorated by {@link HeroClient}. Consumers should use {@link HeroClient}.
 * </p>
 */
@Path("/api/heroes")
@Produces(APPLICATION_JSON)
@RegisterRestClient(configKey = "hero-client")
@RegisterClientHeaders
interface HeroRestClient {
	/**
	 * HTTP <code>GET</code> call to {@code /api/heroes/random} on the Heroes service
	 * @return A {@link Hero}
	 * @see HeroClient#findRandomHero()
	 */
	@GET
	@Path("/random")
	Uni<Hero> findRandomHero();
  
	/**
	 * HTTP <code>GET</code> call to {@code /api/heroes/hello} on the Heroes service
	 * @return A "hello" from Heroes
	 */
  @GET
  @Path("/hello")
  @Produces(TEXT_PLAIN)
  Uni<String> hello();

}
