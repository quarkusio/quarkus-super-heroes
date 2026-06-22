package io.quarkus.sample.superheroes.fight.rest;

import java.util.List;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.fight.api.model.Fight;
import io.quarkus.sample.superheroes.fight.api.model.FightImage;
import io.quarkus.sample.superheroes.fight.api.model.FightLocation;
import io.quarkus.sample.superheroes.fight.api.model.FightRequest;
import io.quarkus.sample.superheroes.fight.api.model.FightToNarrate;
import io.quarkus.sample.superheroes.fight.api.model.Fighters;
import io.quarkus.sample.superheroes.fight.api.model.ImageGenerationRequest;
import io.quarkus.sample.superheroes.fight.api.resources.FightsResource;
import io.quarkus.sample.superheroes.fight.mapping.FightApiMapper;
import io.quarkus.sample.superheroes.fight.service.FightService;

import io.smallrye.mutiny.Uni;

/**
 * JAX-RS API endpoints with {@code /api/fights} as the base URI for all endpoints
 */
public class FightResource implements FightsResource {
  private final FightService service;
  private final FightApiMapper mapper;

  public FightResource(FightService service, FightApiMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @Override
  public Uni<List<Fight>> getAllFights(@DefaultValue("0") Integer page, @DefaultValue("20") Integer size) {
    if (page == null && size == null) {
      return this.service.findAllFights()
        .invoke(fights -> Log.debugf("Total number of fights: %d", fights.size()))
        .map(this.mapper::toApiModels);
    }

    int safePage = Math.max(0, (page != null) ? page : 0);
    int safeSize = Math.min(100, Math.max(1, (size != null) ? size : 20));

    return this.service.findFights(safePage, safeSize)
      .invoke(fights -> Log.debugf("Returned fights page=%d size=%d count=%d", safePage, safeSize, fights.size()))
      .map(this.mapper::toApiModels);
  }

  @Override
  public Uni<Fighters> getRandomFighters() {
    return this.service.findRandomFighters()
      .invoke(fighters -> Log.debugf("Got random fighters: %s", fighters))
      .map(this.mapper::toApiModel);
  }

  @Override
  public Uni<FightLocation> getRandomLocation() {
    return this.service.findRandomLocation()
      .invoke(location -> Log.debugf("Got random location: %s", location))
      .map(this.mapper::toApiModel);
  }

  @Override
  public Uni<Response> getFight(String id) {
    return this.service.findFightById(id)
      .onItem().ifNotNull().transform(f -> {
        Log.debugf("Found fight: %s", f);
        return Response.ok(this.mapper.toApiModel(f)).build();
      })
      .replaceIfNullWith(() -> {
        Log.debugf("No fight found with id %s", id);
        return Response.status(Status.NOT_FOUND).build();
      });
  }

  @Override
  public Uni<Fight> performFight(FightRequest fightRequest) {
    return this.service.performFight(this.mapper.toDomain(fightRequest))
      .map(this.mapper::toApiModel);
  }

  @Override
  public Uni<String> narrateFight(FightToNarrate fightToNarrate) {
    return this.service.narrateFight(this.mapper.toDomain(fightToNarrate));
  }

  @Override
  public Uni<FightImage> generateImageFromNarration(ImageGenerationRequest imageGenerationRequest) {
    return this.service.generateImageFromNarration(this.mapper.toDomain(imageGenerationRequest))
      .invoke(image -> Log.debugf("Image (%s) generated from request: %s", image, imageGenerationRequest))
      .map(this.mapper::toApiModel);
  }
}
