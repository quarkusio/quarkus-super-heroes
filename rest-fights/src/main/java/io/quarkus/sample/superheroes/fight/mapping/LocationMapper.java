package io.quarkus.sample.superheroes.fight.mapping;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

import org.mapstruct.Mapper;

import io.quarkus.sample.superheroes.fight.FightLocation;

@Mapper(componentModel = JAKARTA_CDI)
public interface LocationMapper {
  FightLocation fromGrpc(io.quarkus.sample.superheroes.location.grpc.Location grpcLocation);
}
