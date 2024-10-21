package io.quarkus.sample.superheroes.villain.rest;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.service.VillainService;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

/**
 * JAX-RS API endpoints with <code>/api/villains</code> as the base URI for all endpoints
 */
@Path("/api/villains")
@Tag(name = "villains")
@Produces(APPLICATION_JSON)
public class VillainResource {
  @Inject
	VillainService service;

	@GET
	@Path("/random")
	@Operation(summary = "Returns a random villain")
	@APIResponse(
		responseCode = "200",
		description = "Gets random villain",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Villain.class, required = true),
      examples = @ExampleObject(name = "villain", value = Examples.VALID_EXAMPLE_VILLAIN)
    )
	)
	@APIResponse(
		responseCode = "404",
		description = "No villain found"
	)
  @RunOnVirtualThread
	public Response getRandomVillain() {
		return this.service.findRandomVillain()
			.map(v -> {
				Log.debugf("Found random villain: %s", v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				Log.debug("No random villain found");
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@GET
	@Operation(summary = "Returns all the villains from the database")
	@APIResponse(
		responseCode = "200",
		description = "Gets all villains",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Villain.class, type = SchemaType.ARRAY),
      examples = @ExampleObject(name = "villains", value = Examples.VALID_EXAMPLE_VILLAIN_LIST)
    )
	)
  @RunOnVirtualThread
	public List<Villain> getAllVillains(@Parameter(name = "name_filter", description = "An optional filter parameter to filter results by name") @QueryParam("name_filter") Optional<String> nameFilter) {
    var villains = nameFilter
      .map(this.service::findAllVillainsHavingName)
      .orElseGet(this.service::findAllVillains);

		Log.debugf("Total number of villains: %d", villains.size());

		return villains;
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a villain for a given identifier")
	@APIResponse(
		responseCode = "200",
		description = "Gets a villain for a given id",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Villain.class),
      examples = @ExampleObject(name = "villain", value = Examples.VALID_EXAMPLE_VILLAIN)
    )
	)
	@APIResponse(
		responseCode = "404",
		description = "The villain is not found for a given identifier"
	)
  @RunOnVirtualThread
	public Response getVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		return this.service.findVillainById(id)
			.map(v -> {
				Log.debugf("Found villain: %s", v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Creates a valid villain")
	@APIResponse(
		responseCode = "201",
		description = "The URI of the created villain",
    headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid villain passed in (or no request body found)"
	)
  @RunOnVirtualThread
	public Response createVillain(
    @RequestBody(
      name = "villain",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Villain.class),
        examples = @ExampleObject(name = "valid_villain", value = Examples.VALID_EXAMPLE_VILLAIN_TO_CREATE)
      )
    )
    @Valid @NotNull Villain villain,
    @Context UriInfo uriInfo) {
		var v = this.service.persistVillain(villain);
		var builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(v.id));
		Log.debugf("New villain created with URI %s", builder.build().toString());
		return Response.created(builder.build()).build();
	}

	@PUT
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Completely updates/replaces an exiting villain by replacing it with the passed-in villain")
	@APIResponse(
		responseCode = "204",
		description = "Replaced the villain"
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid villain passed in (or no request body found)"
	)
	@APIResponse(
		responseCode = "404",
		description = "No villain found"
	)
  @RunOnVirtualThread
	public Response fullyUpdateVillain(
    @Parameter(name = "id", required = true) @PathParam("id") Long id,
    @RequestBody(
      name = "villain",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Villain.class),
        examples = @ExampleObject(name = "valid_villain", value = Examples.VALID_EXAMPLE_VILLAIN)
      )
    )
    @Valid @NotNull Villain villain) {
    if (villain.id == null) {
			villain.id = id;
		}

		return this.service.replaceVillain(villain)
			.map(v -> {
				Log.debugf("Villain replaced with new values %s", v);
				return Response.noContent().build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", villain.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @PUT
  @Consumes(APPLICATION_JSON)
  @Operation(summary = "Completely replace all villains with the passed-in villains")
  @APIResponse(
		responseCode = "201",
		description = "The URI to retrieve all the created villains",
		headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid villains passed in (or no request body found)"
	)
  @RunOnVirtualThread
  public Response replaceAllVillains(
    @RequestBody(
      name = "valid_villains",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Villain.class, type = SchemaType.ARRAY),
        examples = @ExampleObject(name = "villains", value = Examples.VALID_EXAMPLE_VILLAIN_LIST)
      )
    )
    @NotNull List<Villain> villains,
    @Context UriInfo uriInfo) {
    this.service.replaceAllVillains(villains);
		var uri = uriInfo.getAbsolutePathBuilder().build();
		Log.debugf("New Villains created with URI %s", uri.toString());
		return Response.created(uri).build();
  }

	@PATCH
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Partially updates an exiting villain")
	@APIResponse(
		responseCode = "200",
		description = "Updated the villain",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Villain.class),
      examples = @ExampleObject(name = "villain", value = Examples.VALID_EXAMPLE_VILLAIN)
    )
	)
	@APIResponse(
		responseCode = "400",
		description = "Null villain passed in (or no request body found)"
	)
	@APIResponse(
		responseCode = "404",
		description = "No villain found"
	)
  @RunOnVirtualThread
	public Response partiallyUpdateVillain(
    @Parameter(name = "id", required = true) @PathParam("id") Long id,
    @RequestBody(
      name = "valid_villain",
      required = true,
      content = @Content(
        schema = @Schema(implementation = Villain.class),
        examples = @ExampleObject(name = "valid_villain", value = Examples.VALID_EXAMPLE_VILLAIN)
      )
    )
    @NotNull Villain villain) {
		if (villain.id == null) {
			villain.id = id;
		}

		return this.service.partialUpdateVillain(villain)
			.map(v -> {
				Log.debugf("Villain updated with new values %s", v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				Log.debugf("No villain found with id %d", villain.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@DELETE
	@Operation(summary = "Delete all villains")
	@APIResponse(
		responseCode = "204",
		description = "Deletes all villains"
	)
  @RunOnVirtualThread
	public void deleteAllVillains() {
		this.service.deleteAllVillains();
		Log.debug("Deleted all villains");
	}

	@DELETE
	@Path("/{id}")
	@Operation(summary = "Deletes an exiting villain")
	@APIResponse(
		responseCode = "204",
		description = "Delete a villain"
	)
  @RunOnVirtualThread
	public void deleteVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		this.service.deleteVillain(id);
		Log.debugf("Villain with id %d deleted ", id);
	}

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	@Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_success", value = "Hello Villain Resource")
    )
	)
  @NonBlocking
	public String hello() {
    Log.debug("Hello Villain Resource");
		return "Hello Villain Resource";
	}
}
