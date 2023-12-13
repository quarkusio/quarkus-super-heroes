package io.quarkus.sample.superheroes.narration.rest;

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
import io.quarkus.sample.superheroes.narration.service.NarrationProcessor;

/**
 * JAX-RS API endpoints with <code>/api/narration</code> as the base URI for all endpoints
 */
@Path("/api/narration")
@Produces(MediaType.TEXT_PLAIN)
@Tag(name = "narration")
public class NarrationResource {
  private final NarrationProcessor narrationProcessor;

	public NarrationResource(NarrationProcessor narrationProcessor) {
		this.narrationProcessor = narrationProcessor;
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
  public String narrate(
    @RequestBody(
      name = "fight",
      required = true,
      content = @Content(
        schema = @Schema(implementation = Fight.class),
        examples = @ExampleObject(name = "valid_fight", value = Examples.EXAMPLE_FIGHT)
      )
    )
    @NotNull Fight fight) {
    var narration = this.narrationProcessor.narrate(fight);
    Log.debugf("Narration for fight %s = \"%s\"", fight, narration);

    return narration;
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
