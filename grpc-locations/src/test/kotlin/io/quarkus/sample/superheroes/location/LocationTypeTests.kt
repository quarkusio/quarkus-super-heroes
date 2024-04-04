package io.quarkus.sample.superheroes.location

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class LocationTypeTests {
  companion object {
    @JvmStatic
    fun toGrpcLocationTypes() = listOf(
      Arguments.of(LocationType.PLANET, io.quarkus.sample.superheroes.location.grpc.LocationType.PLANET),
      Arguments.of(LocationType.CITY, io.quarkus.sample.superheroes.location.grpc.LocationType.CITY),
      Arguments.of(LocationType.PLACE, io.quarkus.sample.superheroes.location.grpc.LocationType.PLACE),
      Arguments.of(LocationType.ISLAND, io.quarkus.sample.superheroes.location.grpc.LocationType.ISLAND),
      Arguments.of(LocationType.COUNTRY, io.quarkus.sample.superheroes.location.grpc.LocationType.COUNTRY),
      Arguments.of(LocationType.MOON, io.quarkus.sample.superheroes.location.grpc.LocationType.MOON),
      Arguments.of(LocationType.OTHER, io.quarkus.sample.superheroes.location.grpc.LocationType.UNRECOGNIZED)
    )

    @JvmStatic
    fun fromGrpcLocationTypes() = toGrpcLocationTypes() + listOf(Arguments.of(LocationType.OTHER, null))
  }

  @ParameterizedTest
  @MethodSource("toGrpcLocationTypes")
  fun `Converting to gRPC location type`(locationType: LocationType, grpcLocationType: io.quarkus.sample.superheroes.location.grpc.LocationType) {
    assertThat(locationType.toGrpcLocationType())
      .isEqualTo(grpcLocationType)
  }

  @ParameterizedTest
  @MethodSource("fromGrpcLocationTypes")
  fun `Converting from gRPC location type`(locationType: LocationType, grpcLocationType: io.quarkus.sample.superheroes.location.grpc.LocationType?) {
    assertThat(LocationType.fromGrpcLocationType(grpcLocationType))
      .isEqualTo(locationType)
  }
}