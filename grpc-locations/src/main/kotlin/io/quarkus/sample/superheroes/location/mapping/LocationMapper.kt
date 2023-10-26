package io.quarkus.sample.superheroes.location.mapping

import io.quarkus.sample.superheroes.location.Location

class LocationMapper {
	companion object {
		fun toGrpcLocation(location: Location): io.quarkus.sample.superheroes.location.grpc.Location =
			io.quarkus.sample.superheroes.location.grpc.Location.newBuilder()
					.setName(location.name)
					.setDescription(location.description)
					.setPicture(location.picture)
					.setType(location.type.toGrpcLocationType())
					.build()

		fun toGrpcLocationMaybeNull(location: Location?) = when(location) {
				null -> null
				else -> toGrpcLocation(location)
			}
	}
}