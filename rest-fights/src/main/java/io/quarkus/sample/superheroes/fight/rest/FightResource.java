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
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.logging.Log;
import io.quarkus.sample.superheroes.fight.Fight;
import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.fight.FightRequest;
import io.quarkus.sample.superheroes.fight.Fighters;
import io.quarkus.sample.superheroes.fight.client.FightToNarrate;
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
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Fight.class, type = ARRAY),
      examples = @ExampleObject(name = "fights", value = "[{\"id\": \"653bea9d188984908cd12429\", \"fightDate\": \"2075-10-27T16:51:41.787Z\", \"winnerName\": \"Luke Skywalker\", \"winnerLevel\": 10, \"winnerPowers\": \"Uses light sabre, The force\", \"winnerPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\", \"loserName\": \"Darth Vader\", \"loserLevel\": 3, \"loserPowers\": \"Uses light sabre, dark side of the force\", \"loserPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\", \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg\"}}]")
    )
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
		description = "Gets a random Hero and Villain fighter",
    content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Fighters.class),
      examples = @ExampleObject(name = "fighters", value = "{\"hero\": {\"name\": \"Luke Skywalker\", \"level\": 10, \"powers\": \"Uses light sabre, The force\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\"}, \"villain\": {\"name\": \"Darth Vader\", \"level\": 3, \"powers\": \"Uses light sabre, dark side of the force\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\"}}")
    )
	)
	public Uni<Fighters> getRandomFighters() {
		return this.service.findRandomFighters()
			.invoke(fighters -> Log.debugf("Got random fighters: %s", fighters));
	}

  @GET
  @Path("/randomlocation")
  @Operation(summary = "Returns a random location")
  @APIResponse(
    responseCode = "200",
    description = "A random location",
    content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = FightLocation.class),
      examples = @ExampleObject(name = "random_location", value = "{\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg\"}")
    )
  )
  public Uni<FightLocation> getRandomFightLocation() {
    return this.service.findRandomLocation()
      .invoke(location -> Log.debugf("Got random location: %s", location));
  }

	@GET
	@Path("/{id}")
	@Operation(summary = "Returns a fight for a given identifier")
	@APIResponse(
		responseCode = "200",
		description = "Gets a fight for a given id",
		content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Fight.class),
      examples = @ExampleObject(name = "fight", value = "{\"id\": \"653bea9d188984908cd12429\", \"fightDate\": \"2075-10-27T16:51:41.787Z\", \"winnerName\": \"Luke Skywalker\", \"winnerLevel\": 10, \"winnerPowers\": \"Uses light sabre, The force\", \"winnerPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\", \"loserName\": \"Darth Vader\", \"loserLevel\": 3, \"loserPowers\": \"Uses light sabre, dark side of the force\", \"loserPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\", \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg\"}}")
    )
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
		description = "The fight",
    content = @Content(
      mediaType = APPLICATION_JSON,
      schema = @Schema(implementation = Fight.class),
      examples = @ExampleObject(name = "fight", value = "{\"id\": \"653bea9d188984908cd12429\", \"fightDate\": \"2075-10-27T16:51:41.787Z\", \"winnerName\": \"Luke Skywalker\", \"winnerLevel\": 10, \"winnerPowers\": \"Uses light sabre, The force\", \"winnerPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\", \"loserName\": \"Darth Vader\", \"loserLevel\": 3, \"loserPowers\": \"Uses light sabre, dark side of the force\", \"loserPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\", \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg\"}}")
    )
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid fighters passed in (or no request body found)"
	)
	public Uni<Fight> perform(
    @RequestBody(
      name = "fight_request",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = FightRequest.class),
        examples = @ExampleObject(name = "fighters", value = "{\"hero\": {\"name\": \"Luke Skywalker\", \"level\": 10, \"powers\": \"Uses light sabre, The force\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\"}, \"villain\": {\"name\": \"Darth Vader\", \"level\": 3, \"powers\": \"Uses light sabre, dark side of the force\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\"}, \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\", \"picture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg\"}}")
      )
    )
    @NotNull @Valid FightRequest fightRequest) {
		return this.service.performFight(fightRequest);
	}

  @POST
  @Path("/narrate")
  @Produces(TEXT_PLAIN)
	@Consumes(APPLICATION_JSON)
	@Operation(summary = "Narrates a fight")
	@APIResponse(
		responseCode = "200",
		description = "The fight narration",
    content = @Content(
      mediaType = TEXT_PLAIN,
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "narration", value = "This is the narration for the fight")
    )
	)
	@APIResponse(
		responseCode = "400",
		description = "Invalid fight passed in (or no request body found)"
	)
	public Uni<String> narrateFight(
    @RequestBody(
      name = "valid_fight",
      required = true,
      content = @Content(
        mediaType = APPLICATION_JSON,
        schema = @Schema(implementation = FightToNarrate.class),
        examples = @ExampleObject(name = "valid_fight", value = "{\"id\": \"653bea9d188984908cd12429\", \"fightDate\": \"2075-10-27T16:51:41.787Z\", \"winnerName\": \"Luke Skywalker\", \"winnerLevel\": 10, \"winnerPowers\": \"Uses light sabre, The force\", \"winnerPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/luke-skywalker-2563509063968639219.jpg\", \"loserName\": \"Darth Vader\", \"loserLevel\": 3, \"loserPowers\": \"Uses light sabre, dark side of the force\", \"loserPicture\": \"https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/anakin-skywalker--8429855148488965479.jpg\", \"location\": {\"name\": \"Gotham City\", \"description\": \"An American city rife with corruption and crime, the home of its iconic protector Batman.\"}}")
      )
    )
    @NotNull @Valid FightToNarrate fight) {
		return this.service.narrateFight(fight);
	}

	@GET
	@Produces(TEXT_PLAIN)
	@Path("/hello")
	@Tag(name = "hello")
	@Operation(summary = "Ping hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_success", value = "Hello Fight Resource")
    )
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
		description = "Ping Heroes hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_heroes_success", value = "Hello Heroes")
    )
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
		description = "Ping Villains hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_villains_success", value = "Hello Villains")
    )
	)
	public Uni<String> helloVillains() {
		return service.helloVillains();
	}

  @GET
  @Produces(TEXT_PLAIN)
  @Path("/hello/narration")
  @Tag(name = "hello")
  @Operation(summary = "Ping Narration hello")
  @APIResponse(
    responseCode = "200",
    description = "Ping Narration hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_narration_success", value = "Hello Narration")
    )
  )
  public Uni<String> helloNarration() {
    return this.service.helloNarration();
  }

	@GET
	@Produces(TEXT_PLAIN)
	@Path("/hello/locations")
	@Tag(name = "hello")
	@Operation(summary = "Ping Locations hello")
	@APIResponse(
		responseCode = "200",
		description = "Ping Locations hello",
    content = @Content(
      schema = @Schema(implementation = String.class),
      examples = @ExampleObject(name = "hello_locations_success", value = "Hello Locations")
    )
	)
	public Uni<String> helloLocations() {
		return this.service.helloLocations();
	}
}
