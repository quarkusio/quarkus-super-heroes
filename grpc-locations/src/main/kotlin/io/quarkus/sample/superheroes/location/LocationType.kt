package io.quarkus.sample.superheroes.location

import io.quarkus.sample.superheroes.location.grpc.LocationType

enum class LocationType {
	PLANET {
		override fun toGrpcLocationType() = LocationType.PLANET
	},
	CITY {
		override fun toGrpcLocationType() = LocationType.CITY
	};

	abstract fun toGrpcLocationType(): LocationType
}