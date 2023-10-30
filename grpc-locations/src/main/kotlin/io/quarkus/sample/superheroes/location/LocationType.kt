package io.quarkus.sample.superheroes.location

import io.quarkus.sample.superheroes.location.grpc.LocationType

enum class LocationType {
	PLANET {
		override fun toGrpcLocationType() = LocationType.PLANET
	},
	CITY {
		override fun toGrpcLocationType() = LocationType.CITY
	},
	OTHER {
		override fun toGrpcLocationType() = LocationType.UNRECOGNIZED
	};

	companion object {
		fun fromGrpcLocationType(locationType: LocationType?) = when(locationType) {
			LocationType.PLANET -> PLANET
			LocationType.CITY -> CITY
			else -> OTHER
		}
	}

	abstract fun toGrpcLocationType(): LocationType
}