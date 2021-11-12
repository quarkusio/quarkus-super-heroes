package io.quarkus.sample.superheroes.villain.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.service.VillainService;

@Path("/api/villains")
@Tag(name = "villains")
@Produces(APPLICATION_JSON)
public class VillainResource {
	private final Logger logger;
	private final VillainService service;

	public VillainResource(Logger logger, VillainService service) {
		this.service = service;
		this.logger = logger;
	}

	@GET
	@Path("/random")
	@Operation(summary = "Returns a random villain")
	@APIResponse(
		responseCode = "200",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Villain.class, required = true))
	)
	public Villain getRandomVillain() {
		Villain villain = this.service.findRandomVillain();
		this.logger.debugf("Found random villain %s", villain);
		return villain;
	}

	@GET
	@Operation(summary = "Returns all the villains from the database")
	@APIResponse(
		responseCode = "200",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Villain.class, type = SchemaType.ARRAY))
	)
	@APIResponse(responseCode = "204", description = "No villains")
	public Response getAllVillains() {
		List<Villain> villains = Optional.ofNullable(this.service.findAllVillains())
			.orElseGet(List::of);

		this.logger.debugf("Total number of villains %d", villains.size());

		return !villains.isEmpty() ?
		       Response.ok(villains).build() :
		       Response.noContent().build();
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a villain for a given identifier")
	@APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Villain.class)))
	@APIResponse(responseCode = "404", description = "The villain is not found for a given identifier")
	public Response getVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		return this.service.findVillainById(id)
			.map(v -> {
				this.logger.debugf("Found villain %s", v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				this.logger.debugf("No villain found with id %d", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Creates a valid villain")
	@APIResponse(
		responseCode = "201",
		description = "The URI of the created villain",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = URI.class))
	)
	@APIResponse(responseCode = "400", description = "Invalid villain passed in")
	public Response createVillain(@Valid @NotNull Villain villain, @Context UriInfo uriInfo) {
		Villain v = this.service.persistVillain(villain);
		UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(v.id));
		this.logger.debugf("New villain created with URI %s", builder.build().toString());
		return Response.created(builder.build()).build();
	}

	@PUT
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Completely updates/replaces an exiting villain by replacing it with the passed-in villain")
	@APIResponse(responseCode = "204", description = "Replaced the villain")
	@APIResponse(responseCode = "400", description = "Invalid villain passed in")
	@APIResponse(responseCode = "404", description = "No villain found")
	public Response fullyUpdateVillain(@Valid @NotNull Villain villain) {
		return this.service.replaceVillain(villain)
			.map(v -> {
				this.logger.debugf("Villain replaced with new values %s", v);
				return Response.noContent().build();
			})
			.orElseGet(() -> {
				this.logger.debugf("No villain found with id %d", villain.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@PATCH
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Partially updates an exiting villain")
	@APIResponse(responseCode = "200", description = "Updated the villain", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Villain.class)))
	@APIResponse(responseCode = "400", description = "Null villain passed in (no request body)")
	@APIResponse(responseCode = "404", description = "No villain found")
	public Response partiallyUpdateVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id, @NotNull Villain villain) {
		if (villain.id == null) {
			villain.id = id;
		}

		return this.service.partialUpdateVillain(villain)
			.map(v -> {
				this.logger.debugf("Villain updated with new values %s", v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				this.logger.debugf("No villain found with id %d", villain.id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@DELETE
	@Path("/{id}")
	@Operation(summary = "Deletes an exiting villain")
	@APIResponse(responseCode = "204", description = "Delete a villain")
	public void deleteVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		this.service.deleteVillain(id);
		this.logger.debugf("Villain with id %d deleted ", id);
	}

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	@Tag(name = "hello")
	public String hello() {
		return "Hello Villain Resource";
	}
}
