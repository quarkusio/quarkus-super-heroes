package io.quarkus.sample.superheroes.fight.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import io.quarkus.sample.superheroes.fight.FightLocation;
import io.quarkus.sample.superheroes.location.grpc.LocationType;

class LocationMapperTests {
  private static final String DEFAULT_LOCATION_NAME = "Gotham City";
  private static final String DEFAULT_LOCATION_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman.";
  private static final String DEFAULT_LOCATION_PICTURE = "gotham_city.png";
  private static final io.quarkus.sample.superheroes.location.grpc.Location GRPC_LOCATION = io.quarkus.sample.superheroes.location.grpc.Location.newBuilder()
    .setName(DEFAULT_LOCATION_NAME)
    .setDescription(DEFAULT_LOCATION_DESCRIPTION)
    .setPicture(DEFAULT_LOCATION_PICTURE)
    .setType(LocationType.PLANET)
    .build();

  LocationMapper mapper = Mappers.getMapper(LocationMapper.class);

  @Test
  public void mappingWorks() {
    assertThat(this.mapper.fromGrpc(GRPC_LOCATION))
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new FightLocation(DEFAULT_LOCATION_NAME, DEFAULT_LOCATION_DESCRIPTION, DEFAULT_LOCATION_PICTURE));
  }
}
