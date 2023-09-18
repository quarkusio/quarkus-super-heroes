package io.quarkus.sample.superheroes.fight.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.smallrye.mutiny.Uni;

/**
 * <a href="https://quarkus.io/guides/rest-client-reactive">Quarkus Reactive Rest Client</a> that talks to the Narration service.
 */
@Path("/api/narration")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "narration-client")
@RegisterClientHeaders
public interface NarrationClient {
  /**
   * HTTP <code>POST</code> call to {@code /api/narration} on the Narration service
   * @param fight The {@link io.quarkus.sample.superheroes.fight.Fight Fight} to narrate
   * @return The narration for the fight
   */
  @POST
  @WithSpan(kind = SpanKind.CLIENT, value = "NarrationClient.narrateFight")
  Uni<String> narrate(@SpanAttribute("arg.fight") FightToNarrate fight);

  /**
   * HTTP <code>GET</code> call to {@code /api/narration/hello} on the Narration service
   * @return A "hello" from Narration
   */
  @GET
  @Path("/hello")
  Uni<String> hello();
}
