package io.quarkus.sample.superheroes.location.mapping

import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.LocationType
import io.quarkus.sample.superheroes.location.grpc.location

class LocationMapper {
	companion object {
		fun toGrpcLocation(location: Location) = location {
      name = location.name
      description = location.description ?: ""
      picture = location.picture ?: ""
      type = location.type.toGrpcLocationType()
    }

		fun fromGrpcLocation(location: io.quarkus.sample.superheroes.location.grpc.Location): Location {
			val l = Location()
			l.name = location.name
			l.description = location.description
			l.picture = location.picture
			l.type = LocationType.fromGrpcLocationType(location.type)

			return l
		}

		fun toGrpcLocationMaybeNull(location: Location?) = when(location) {
				null -> null
				else -> toGrpcLocation(location)
			}
	}
}
