package io.quarkus.sample.superheroes.villain.rest;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
		this.logger.debug("Found random villain " + villain);
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
		List<Villain> villains = this.service.findAllVillains();
		this.logger.debug("Total number of villains " + villains);

		return ((villains != null) && !villains.isEmpty()) ?
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
				this.logger.debug("Found villain " + v);
				return Response.ok(v).build();
			})
			.orElseGet(() -> {
				this.logger.debug("No villain found with id " + id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@POST
	@Operation(summary = "Creates a valid villain")
	@APIResponse(
		responseCode = "201",
		description = "The URI of the created villain",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = URI.class))
	)
	public Response createVillain(@Valid Villain villain, @Context UriInfo uriInfo) {
		Villain v = this.service.persistVillain(villain);
		UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(v.id));
		this.logger.debug("New villain created with URI " + builder.build().toString());
		return Response.created(builder.build()).build();
	}

	@PUT
	@Operation(summary = "Updates an exiting  villain")
	@APIResponse(
		responseCode = "200",
		description = "The updated villain",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Villain.class))
	)
	public Villain updateVillain(@Valid Villain villain) {
		Villain v = this.service.updateVillain(villain);
		this.logger.debug("Villain updated with new valued " + v);
		return v;
	}

	@DELETE
	@Path("/{id}")
	@Operation(summary = "Deletes an exiting villain")
	@APIResponse(responseCode = "204")
	public void deleteVillain(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		this.service.deleteVillain(id);
		this.logger.debug("Villain deleted with " + id);
	}

	@GET
	@Path("/hello")
	@Produces(MediaType.TEXT_PLAIN)
	@Tag(name = "hello")
	public String hello() {
		return "Hello Villain Resource";
	}

	@GET
	@Path("/test")
	public Villain test() {
		return this.service.persistVillain(null);
	}
}
