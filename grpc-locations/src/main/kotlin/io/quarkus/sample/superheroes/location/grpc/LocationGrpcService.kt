package io.quarkus.sample.superheroes.location.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.smallrye.common.annotation.Blocking
import io.quarkus.grpc.GrpcService
import io.quarkus.logging.Log
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsImplBase
import io.quarkus.sample.superheroes.location.service.LocationService

@GrpcService
class LocationGrpcService(private val locationService: LocationService) : LocationsImplBase() {
  @Blocking
  override fun getRandomLocation(request: RandomLocationRequest?, responseObserver: StreamObserver<Location>?) {
		Log.debug("Requesting a random location")
		val location = this.locationService.getRandomLocation()

    if (location != null) {
			Log.info("Random location found: $location")
			responseObserver?.onNext(convertLocation(location))
    }
    else {
			Log.info("No random location was found")
			responseObserver?.onError(
				Status.NOT_FOUND
					.withDescription("A random location was not found")
					.asException()
			)
    }

	  responseObserver?.onCompleted()
  }

	@Blocking
	override fun getAllLocations(request: AllLocationsRequest?, responseObserver: StreamObserver<AllLocationsResponse>?) {
		val allLocations = this.locationService.getAllLocations()
		Log.debug("Got all locations: $allLocations")

		responseObserver?.onNext(
			AllLocationsResponse.newBuilder()
				.addAllLocations(allLocations.map(::convertLocation))
				.build())
		responseObserver?.onCompleted()
	}

	@Blocking
	override fun deleteAllLocations(request: DeleteAllLocationsRequest?, responseObserver: StreamObserver<DeleteAllLocationsResponse>?) {
		Log.debug("Deleting all locations")

		this.locationService.deleteAllLocations()
		responseObserver?.onNext(DeleteAllLocationsResponse.newBuilder().build())
		responseObserver?.onCompleted()
	}

	override fun hello(request: HelloRequest?, responseObserver: StreamObserver<HelloReply>?) {
		Log.debug("Hello Location service")

		responseObserver?.onNext(HelloReply.newBuilder().setMessage("Hello Location Service").build())
		responseObserver?.onCompleted()
	}

	private fun convertLocation(location: io.quarkus.sample.superheroes.location.Location) : Location {
    return Location.newBuilder()
              .setName(location.name)
              .setDescription(location.description)
              .setPicture(location.picture)
              .build()
  }
}
