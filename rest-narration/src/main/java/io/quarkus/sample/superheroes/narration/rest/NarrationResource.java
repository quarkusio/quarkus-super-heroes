package io.quarkus.sample.superheroes.narration.rest;

import jakarta.enterprise.inject.Instance;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.service.NarrationService;

import io.smallrye.mutiny.Uni;

/**
 * JAX-RS API endpoints with <code>/api/narration</code> as the base URI for all endpoints
 */

@Path("/api/narration")
@Produces(MediaType.TEXT_PLAIN)
@Tag(name = "narration")
public class NarrationResource {
  private final NarrationService narrationService;

  public NarrationResource(Instance<NarrationService> narrationService) {
    this.narrationService = narrationService.get();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Creates a narration for the fight")
  @APIResponse(
    responseCode = "200",
    description = "The narration"
  )
  public Uni<String> narrate(@NotNull Fight fight) {
    return this.narrationService.narrate(fight)
      .invoke(narration -> Log.debugf("Narration for fight %s = \"%s\"", fight, narration));
  }

  @GET
  @Path("/hello")
  @Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello"
	)
  public String hello() {
    Log.debug("Hello Narration Resource");
    return "Hello Narration Resource";
  }
}
