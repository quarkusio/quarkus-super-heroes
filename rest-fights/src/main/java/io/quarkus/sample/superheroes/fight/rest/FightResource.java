package io.quarkus.sample.superheroes.fight.rest;

import static jakarta.ws.rs.core.MediaType.*;
import static org.eclipse.microprofile.openapi.annotations.enums.SchemaType.ARRAY;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.Hero;
import io.quarkus.sample.superheroes.fight.service.FightService;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

/**
 * JAX-RS API endpoints with {@code /api/fights} as the base URI for all endpoints
 */
@Path("/api/fights")
@Produces(APPLICATION_JSON)
@Tag(name = "fights")
public class FightResource {
	private final FightService service;

	public FightResource(FightService service) {
		this.service = service;
	}

	@GET
	@Operation(summary = "Returns all the fights")
	@APIResponse(
		responseCode = "200",
		description = "Gets all fights, or empty list if none",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Fight.class, type = ARRAY))
	)
	public Uni<List<Fight>> getAllFights() {
		return this.service.findAllFights()
			.invoke(fights -> Log.debugf("Total number of fights: %d", fights.size()));
	}

	@GET
	@Path("/randomfighters")
	@Operation(summary = "Returns random fighters")
	@APIResponse(
		responseCode = "200",
		description = "Gets a random Hero and Villain fighter"
	)
	public Uni<Fighters> getRandomFighters() {
		return this.service.findRandomFighters()
			.invoke(fighters -> Log.debugf("Got random fighters: %s", fighters));
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a fight for a given identifier")
	@APIResponse(
		responseCode = "200",
		description = "Gets a fight for a given id",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class))
	)
	@APIResponse(
		responseCode = "404",
		description = "The fight is not found for a given identifier"
	)
	public Uni<Response> getFight(@Parameter(name = "id", required = true) @PathParam("id") String id) {
		return this.service.findFightById(id)
			.onItem().ifNotNull().transform(f -> {
				Log.debugf("Found fight: %s", f);
				return Response.ok(f).build();
			})
			.onItem().ifNull().continueWith(() -> {
				Log.debugf("No fight found with id %s", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Initiates a fight")
	@APIResponse(
		responseCode = "200",
		description = "The fight"
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid fighters passed in (or no request body found)"
	)
	public Uni<Fight> perform(@NotNull @Valid Fighters fighters) {
		return this.service.performFight(fighters);
	}

	@GET
	@Produces(TEXT_PLAIN)
	@Path("/hello")
	@Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello"
	)
	@NonBlocking
	public String hello() {
		return "Hello Fight Resource";
	}

	@GET
	@Produces(TEXT_PLAIN)
	@Path("/hello/heroes")
	@Tag(name = "hello")
	@Operation(summary = "Ping Heroes hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping Heroes hello"
	)
	public Uni<String> helloHeroes() {
		return service.helloHeroes();
	}

	@GET
	@Produces(TEXT_PLAIN)
	@Path("/hello/villains")
	@Tag(name = "hello")
	@Operation(summary = "Ping Villains hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping Villains hello"
	)
	public Uni<String> helloVillains() {
		return service.helloVillains();
	}
}
