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
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
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
    description = "The narration",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "narration_success", value = "This is your fight narration!")
    )
  )
  @APIResponse(
    responseCode = "400",
    description = "Invalid (or missing) fight"
  )
  public Uni<String> narrate(
    @RequestBody(
      name = "fight",
      required = true,
      content = @Content(
        schema = @Schema(implementation = Fight.class),
        examples = @ExampleObject(name = "valid_fight", value = "{\"winnerName\": \"Chewbacca\", \"winnerLevel\": 5, \"winnerPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/chewbacca--684239239428094811.jpg\", \"winnerPowers\": \"Big, hairy, strong\", \"winnerTeam\": \"heroes\", \"loserName\": \"Wanderer\", \"loserLevel\": 3, \"loserPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/wanderer-300775911119209178.jpg\", \"loserPowers\": \"Not strong\", \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\"}}")
      )
    )
    @NotNull Fight fight) {
    return this.narrationService.narrate(fight)
      .invoke(narration -> Log.debugf("Narration for fight %s = \"%s\"", fight, narration));
  }

  @GET
  @Path("/hello")
  @Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_success", value = "Hello Narration Resource")
    )
	)
  public String hello() {
    Log.debug("Hello Narration Resource");
    return "Hello Narration Resource";
  }
}
