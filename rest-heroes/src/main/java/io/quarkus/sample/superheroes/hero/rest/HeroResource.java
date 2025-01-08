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
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import io.quarkus.logging.Log;

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
	private final HeroService heroService;

	public HeroResource(HeroService heroService) {
		this.heroService = heroService;
	}

	@GET
	@Path("/random")
	@Operation(summary = "Returns a random hero")
	@APIResponse(
		responseCode = "200",
		description = "Gets a random hero",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Hero.class, required = true),
      examples = @ExampleObject(name = "hero", value = Examples.VALID_EXAMPLE_HERO)
    )
	)
	@APIResponse(
		responseCode = "404",
		description = "No hero found"
	)
	public Uni<Response> getRandomHero() {
		return this.heroService.findRandomHero()
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Found random hero: %s", h);
				return Response.ok(h).build();
			})
			.replaceIfNullWith(() -> {
				Log.debug("No random villain found");
				return Response.status(Status.NOT_FOUND).build();
			});
	}

	@GET
	@Operation(summary = "Returns all the heroes from the database")
	@APIResponse(
		responseCode = "200",
		description = "Gets all heroes",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Hero.class, type = SchemaType.ARRAY),
      examples = @ExampleObject(name = "heroes", value = Examples.VALID_EXAMPLE_HERO_LIST)
    )
	)
	public Uni<List<Hero>> getAllHeroes(@Parameter(name = "name_filter", description = "An optional filter parameter to filter results by name") @QueryParam("name_filter") Optional<String> nameFilter) {
    return nameFilter
      .map(this.heroService::findAllHeroesHavingName)
      .orElseGet(() -> this.heroService.findAllHeroes().replaceIfNullWith(List::of))
      .invoke(heroes -> Log.debugf("Total number of heroes: %d", heroes.size()));
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a hero for a given identifier")
	@APIResponse(
		responseCode = "200",
		description = "Gets a hero for a given id",
		content = @Content(
      mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class),
      examples = @ExampleObject(name = "hero", value = Examples.VALID_EXAMPLE_HERO)
    )
	)
	@APIResponse(
		responseCode = "404",
		description = "The hero is not found for a given identifier"
	)
	public Uni<Response> getHero(@Parameter(name = "id", required = true) @PathParam("id") Long id) {
		return this.heroService.findHeroById(id)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Found hero: %s", h);
				return Response.ok(h).build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", id);
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
	public Uni<Response> createHero(
    @RequestBody(
      name = "hero",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Hero.class),
        examples = @ExampleObject(name = "valid_hero", value = Examples.VALID_EXAMPLE_HERO_TO_CREATE)
      )
    )
    @Valid @NotNull Hero hero, @
    Context UriInfo uriInfo) {
		return this.heroService.persistHero(hero)
			.map(h -> {
				var uri = uriInfo.getAbsolutePathBuilder().path(Long.toString(h.getId())).build();
				Log.debugf("New Hero created with URI %s", uri.toString());
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
	public Uni<Response> fullyUpdateHero(
		@Parameter(name = "id", required = true) @PathParam("id") Long id,
    @RequestBody(
      name = "hero",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Hero.class),
        examples = @ExampleObject(name = "valid_hero", value = Examples.VALID_EXAMPLE_HERO)
      )
    )
    @Valid @NotNull Hero hero) {
    if (hero.getId() == null) {
			hero.setId(id);
		}

		return this.heroService.replaceHero(hero)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Hero replaced with new values %s", h);
				return Response.noContent().build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", hero.getId());
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
  public Uni<Response> replaceAllHeroes(
    @RequestBody(
      name = "valid_villains",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = Hero.class, type = SchemaType.ARRAY),
        examples = @ExampleObject(name = "heroes", value = Examples.VALID_EXAMPLE_HERO_LIST)
      )
    )
    @NotNull List<Hero> heroes,
    @Context UriInfo uriInfo) {
    return this.heroService.replaceAllHeroes(heroes)
      .map(h -> {
				var uri = uriInfo.getAbsolutePathBuilder().build();
				Log.debugf("New Heroes created with URI %s", uri.toString());
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
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Hero.class),
      examples = @ExampleObject(name = "hero", value = Examples.VALID_EXAMPLE_HERO)
    )
	)
	@APIResponse(
		responseCode = "400",
		description = "Null hero passed in (or no request body found)"
	)
	@APIResponse(
		responseCode = "404",
		description = "No hero found"
	)
	public Uni<Response> partiallyUpdateHero(
    @Parameter(name = "id", required = true) @PathParam("id") Long id,
    @RequestBody(
      name = "valid_hero",
      required = true,
      content = @Content(
        schema = @Schema(implementation = Hero.class),
        examples = @ExampleObject(name = "valid_hero", value = Examples.VALID_EXAMPLE_HERO)
      )
    )
    @NotNull Hero hero) {
		if (hero.getId() == null) {
			hero.setId(id);
		}

		return this.heroService.partialUpdateHero(hero)
			.onItem().ifNotNull().transform(h -> {
				Log.debugf("Hero updated with new values %s", h);
				return Response.ok(h).build();
			})
			.replaceIfNullWith(() -> {
				Log.debugf("No hero found with id %d", hero.getId());
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
			.invoke(() -> Log.debug("Deleted all heroes"));
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
			.invoke(() -> Log.debugf("Hero deleted with %d", id));
	}

	@GET
	@Path("/hello")
	@Produces(TEXT_PLAIN)
	@Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_success", value = "Hello Hero Resource")
    )
	)
	@NonBlocking
	public String hello() {
    Log.debug("Hello Hero Resource");
		return "Hello Hero Resource";
	}
}
