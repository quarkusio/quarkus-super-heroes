package io.quarkus.sample.superheroes.location

import io.quarkus.sample.superheroes.location.grpc.LocationType

enum class LocationType {
	PLANET {
		override fun toGrpcLocationType() = LocationType.PLANET
	},
	CITY {
		override fun toGrpcLocationType() = LocationType.CITY
	},
	PLACE {
		override fun toGrpcLocationType() = LocationType.PLACE
	},
	ISLAND {
		override fun toGrpcLocationType() = LocationType.ISLAND
	},
	COUNTRY {
		override fun toGrpcLocationType() = LocationType.COUNTRY
	},
	MOON {
		override fun toGrpcLocationType() = LocationType.MOON
	},
	OTHER {
		override fun toGrpcLocationType() = LocationType.UNRECOGNIZED
	};

	companion object {
		fun fromGrpcLocationType(locationType: LocationType?) = when(locationType) {
			LocationType.PLANET -> PLANET
			LocationType.CITY -> CITY
			LocationType.PLACE -> PLACE
			LocationType.ISLAND -> ISLAND
			LocationType.COUNTRY -> COUNTRY
			LocationType.MOON -> MOON
			else -> OTHER
		}
	}

	abstract fun toGrpcLocationType(): LocationType
}