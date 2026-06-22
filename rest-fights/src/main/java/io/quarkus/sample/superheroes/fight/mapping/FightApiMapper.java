package io.quarkus.sample.superheroes.fight.mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.sample.superheroes.fight.api.model.FightToNarrateLocation;

/**
 * MapStruct mapper for converting between generated API model types and domain types.
 */
@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface FightApiMapper {
  // Fight entity -> API model Fight
  io.quarkus.sample.superheroes.fight.api.model.Fight toApiModel(io.quarkus.sample.superheroes.fight.Fight entity);

  // List<Fight entity> -> List<API model Fight>
  List<io.quarkus.sample.superheroes.fight.api.model.Fight> toApiModels(List<io.quarkus.sample.superheroes.fight.Fight> entities);

  // API model FightRequest -> domain FightRequest
  io.quarkus.sample.superheroes.fight.FightRequest toDomain(io.quarkus.sample.superheroes.fight.api.model.FightRequest apiModel);

  // domain Fighters -> API model Fighters
  io.quarkus.sample.superheroes.fight.api.model.Fighters toApiModel(io.quarkus.sample.superheroes.fight.Fighters domain);

  // API model FightLocation <-> domain FightLocation
  io.quarkus.sample.superheroes.fight.FightLocation toDomain(io.quarkus.sample.superheroes.fight.api.model.FightLocation apiModel);
  io.quarkus.sample.superheroes.fight.api.model.FightLocation toApiModel(io.quarkus.sample.superheroes.fight.FightLocation domain);

  // API model FightToNarrate -> domain FightToNarrate
  io.quarkus.sample.superheroes.fight.client.FightToNarrate toDomain(io.quarkus.sample.superheroes.fight.api.model.FightToNarrate apiModel);

  // API model FightToNarrateLocation -> domain FightToNarrate.FightToNarrateLocation
  io.quarkus.sample.superheroes.fight.client.FightToNarrate.FightToNarrateLocation toDomain(FightToNarrateLocation apiModel);

  // API model FightImage <-> domain FightImage
  io.quarkus.sample.superheroes.fight.FightImage toDomain(io.quarkus.sample.superheroes.fight.api.model.FightImage apiModel);
  io.quarkus.sample.superheroes.fight.api.model.FightImage toApiModel(io.quarkus.sample.superheroes.fight.FightImage domain);

  // API model ImageGenerationRequest -> domain ImageGenerationRequest
  io.quarkus.sample.superheroes.fight.ImageGenerationRequest toDomain(io.quarkus.sample.superheroes.fight.api.model.ImageGenerationRequest apiModel);

  // API model Hero <-> domain Hero
  io.quarkus.sample.superheroes.fight.client.Hero toDomain(io.quarkus.sample.superheroes.fight.api.model.Hero apiModel);
  io.quarkus.sample.superheroes.fight.api.model.Hero toApiModel(io.quarkus.sample.superheroes.fight.client.Hero domain);

  // API model Villain <-> domain Villain
  io.quarkus.sample.superheroes.fight.client.Villain toDomain(io.quarkus.sample.superheroes.fight.api.model.Villain apiModel);
  io.quarkus.sample.superheroes.fight.api.model.Villain toApiModel(io.quarkus.sample.superheroes.fight.client.Villain domain);

  // ObjectId -> String mapping for Fight entity id
  default String map(ObjectId id) {
    return (id != null) ? id.toString() : null;
  }

  // Instant -> OffsetDateTime mapping for Fight entity fightDate
  default OffsetDateTime map(Instant instant) {
    return (instant != null) ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
