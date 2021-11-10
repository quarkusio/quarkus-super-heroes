package io.quarkus.workshop.superheroes.hero;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestPath;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/heroes")
@Tag(name = "heroes")
@ApplicationScoped
public class HeroResource {

    Logger logger;

    public HeroResource(Logger logger) {
        this.logger = logger;
    }

    @Operation(summary = "Returns a random hero")
    @GET
    @Path("/random")
    @APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class, required = true)))
    public Uni<Hero> getRandomHero() {
        return Hero.findRandom()
            .invoke(h -> logger.debugf("Found random hero: %s", h));
    }

    @Operation(summary = "Returns all the heroes from the database")
    @GET
    @APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "204", description = "No Heroes")
    public Uni<List<Hero>> getAllHeroes() {
        return Hero.listAll();
    }

    @Operation(summary = "Returns a hero for a given identifier")
    @GET
    @Path("/{id}")
    @APIResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class)))
    @APIResponse(responseCode = "204", description = "The hero is not found for a given identifier")
    public Uni<Response> getHero(@RestPath Long id) {
        return Hero.<Hero>findById(id)
            .map(hero -> {
                if (hero != null) {
                    return Response.ok(hero).build();
                }
                logger.debugf("No Hero found with id %d", id);
                return Response.noContent().build();
            });
    }

    @Operation(summary = "Creates a valid hero")
    @POST
    @APIResponse(responseCode = "201", description = "The URI of the created hero", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = URI.class)))
    @ReactiveTransactional
    public Uni<Response> createHero(@Valid Hero hero, @Context UriInfo uriInfo) {
        return hero.<Hero>persist()
            .map(h -> {
                UriBuilder builder = uriInfo.getAbsolutePathBuilder().path(Long.toString(h.id));
                logger.debug("New Hero created with URI " + builder.build().toString());
                return Response.created(builder.build()).build();
            });
    }

    @Operation(summary = "Updates an exiting hero")
    @PUT
    @APIResponse(responseCode = "200", description = "The updated hero", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Hero.class)))
    @ReactiveTransactional
    public Uni<Response> updateHero(@Valid Hero hero) {
        return Hero.<Hero>findById(hero.id)
            .map(retrieved -> {
                retrieved.name = hero.name;
                retrieved.otherName = hero.otherName;
                retrieved.level = hero.level;
                retrieved.picture = hero.picture;
                retrieved.powers = hero.powers;
                return retrieved;
            })
            .map(h -> {
                logger.debugf("Hero updated with new valued %s", h);
                return Response.ok(h).build();
            });

    }

    @Operation(summary = "Deletes an exiting hero")
    @DELETE
    @Path("/{id}")
    @APIResponse(responseCode = "204")
    @ReactiveTransactional
    public Uni<Response> deleteHero(@RestPath Long id) {
        return Hero.deleteById(id)
            .invoke(() -> logger.debugf("Hero deleted with %d", id))
            .replaceWith(Response.noContent().build());
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/hello")
    @Tag(name = "hello")
    public String hello() {
        return "Hello Hero Resource";
    }
}
