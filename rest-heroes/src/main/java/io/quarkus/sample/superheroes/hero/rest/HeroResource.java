package io.quarkus.sample.superheroes.hero.rest;

import static jakarta.ws.rs.core.MediaType.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.validation.ConstraintViolationException;
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.service.HeroService;

import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

/**
 * JAX-RS API endpoints with <code>/api/heroes</code> as the base URI for all endpoints
 */
@Path("/api/heroes")
@Tag(name = "heroes")
@Produces(APPLICATION_JSON)
public class HeroResource {
	private final Logger logger;
	private final HeroService heroService;

	public HeroResource(Logger logger, HeroService heroService) {
		this.logger = logger;
		this.heroService = heroService;
	}

	@GET
	@Path("/random")
	@Operation(summary = "Returns a random hero")
	@APIResponse(
		responseCode = "200",
		description = "Gets a random hero",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class, required = true))
	)
	@APIResponse(
		responseCode = "404",
		description = "No hero found"
	)
	public Uni<Response> getRandomHero() {
		return this.heroService.findRandomHero()
			.onItem().ifNotNull().transform(h -> {
				this.logger.debugf("Found random hero: %s", h);
				return Response.ok(h).build();
			})
			.onItem().ifNull().continueWith(() -> {
				this.logger.debug("No random villain found");
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@GET
	@Operation(summary = "Returns all the heroes from the database")
	@APIResponse(
		responseCode = "200",
		description = "Gets all heroes",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class, type = SchemaType.ARRAY))
	)
	public Uni<List<Hero>> getAllHeroes(@Parameter(name = "name_filter", description = "An optional filter parameter to filter results by name") @QueryParam("name_filter") Optional<String> nameFilter) {
    return nameFilter
      .map(this.heroService::findAllHeroesHavingName)
      .orElseGet(this.heroService::findAllHeroes)
      .invoke(heroes -> this.logger.debugf("Total number of heroes: %d", heroes.size()));
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a hero for a given identifier")
	@APIResponse(
		responseCode = "200",
		description = "Gets a hero for a given id",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class))
	)
	@APIResponse(
		responseCode = "404",
		description = "The hero is not found for a given identifier"
	)
	public Uni<Response> getHero(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		return this.heroService.findHeroById(id)
			.onItem().ifNotNull().transform(h -> {
				this.logger.debugf("Found hero: %s", h);
				return Response.ok(h).build();
			})
			.onItem().ifNull().continueWith(() -> {
				this.logger.debugf("No hero found with id %d", id);
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Creates a valid hero")
	@APIResponse(
		responseCode = "201",
		description = "The URI of the created hero",
		headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid hero passed in (or no request body found)"
	)
	public Uni<Response> createHero(@Valid @NotNull Hero hero, @Context UriInfo uriInfo) {
		return this.heroService.persistHero(hero)
			.map(h -> {
				var uri = uriInfo.getAbsolutePathBuilder().path(Long.toString(h.getId())).build();
				this.logger.debugf("New Hero created with URI %s", uri.toString());
				return Response.created(uri).build();
			});
	}

	@PUT
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Completely updates/replaces an exiting hero by replacing it with the passed-in hero")
	@APIResponse(
		responseCode = "204",
		description = "Replaced the hero"
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid hero passed in (or no request body found)"
	)
	@APIResponse(
		responseCode = "404",
		description = "No hero found"
	)
	public Uni<Response> fullyUpdateHero(@Parameter(name = "id", required = true) @PathParam("id") Long id, @Valid @NotNull Hero hero) {
    if (hero.getId() == null) {
			hero.setId(id);
		}

		return this.heroService.replaceHero(hero)
			.onItem().ifNotNull().transform(h -> {
				this.logger.debugf("Hero replaced with new values %s", h);
				return Response.noContent().build();
			})
			.onItem().ifNull().continueWith(() -> {
				this.logger.debugf("No hero found with id %d", hero.getId());
				return Response.status(Status.NOT_FOUND).build();
			});
	}

  @PUT
  @Consumes(APPLICATION_JSON)
  @Operation(summary = "Completely replace all heroes with the passed-in heroes")
  @APIResponse(
		responseCode = "201",
		description = "The URI to retrieve all the created heroes",
		headers = @Header(name = HttpHeaders.LOCATION, schema = @Schema(implementation = URI.class))
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid heroes passed in (or no request body found)"
	)
  public Uni<Response> replaceAllHeroes(@NotNull List<Hero> heroes, @Context UriInfo uriInfo) {
    return this.heroService.replaceAllHeroes(heroes)
      .map(h -> {
				var uri = uriInfo.getAbsolutePathBuilder().build();
				this.logger.debugf("New Heroes created with URI %s", uri.toString());
				return Response.created(uri).build();
			});
  }

	@PATCH
	@Path("/{id}")
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Partially updates an exiting hero")
	@APIResponse(
		responseCode = "200",
		description = "Updated the hero",
		content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class))
	)
	@APIResponse(
		responseCode = "400",
		description = "Null hero passed in (or no request body found)"
	)
	@APIResponse(
		responseCode = "404",
		description = "No hero found"
	)
	public Uni<Response> partiallyUpdateHero(@Parameter(name = "id", required = true) @PathParam("id") Long id, @NotNull Hero hero) {
		if (hero.getId() == null) {
			hero.setId(id);
		}

		return this.heroService.partialUpdateHero(hero)
			.onItem().ifNotNull().transform(h -> {
				this.logger.debugf("Hero updated with new values %s", h);
				return Response.ok(h).build();
			})
			.onItem().ifNull().continueWith(() -> {
				this.logger.debugf("No hero found with id %d", hero.getId());
				return Response.status(Status.NOT_FOUND).build();
			})
			.onFailure(ConstraintViolationException.class)
			.transform(cve -> new ResteasyReactiveViolationException(((ConstraintViolationException) cve).getConstraintViolations()));
	}

	@DELETE
	@Operation(summary = "Delete all heroes")
	@APIResponse(
		responseCode = "204",
		description = "Deletes all heroes"
	)
	public Uni<Void> deleteAllHeros() {
		return this.heroService.deleteAllHeroes()
			.invoke(() -> this.logger.debug("Deleted all heroes"));
	}

	@DELETE
	@Path("/{id}")
	@Operation(summary = "Deletes an exiting hero")
	@APIResponse(
		responseCode = "204",
		description = "Deletes a hero"
	)
	public Uni<Void> deleteHero(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		return this.heroService.deleteHero(id)
			.invoke(() -> this.logger.debugf("Hero deleted with %d", id));
	}

	@GET
	@Path("/hello")
	@Produces(TEXT_PLAIN)
	@Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello"
	)
	@NonBlocking
	public String hello() {
    this.logger.debug("Hello Hero Resource");
		return "Hello Hero Resource";
	}
}
