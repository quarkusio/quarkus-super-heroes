package io.quarkus.sample.superheroes.location.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.smallrye.common.annotation.Blocking
import io.quarkus.grpc.GrpcService
import io.quarkus.logging.Log
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsImplBase
import io.quarkus.sample.superheroes.location.mapping.LocationMapper
import io.quarkus.sample.superheroes.location.service.LocationService

@GrpcService
class LocationGrpcService(private val locationService: LocationService) : LocationsImplBase() {
  @Blocking
  override fun getRandomLocation(request: RandomLocationRequest?, responseObserver: StreamObserver<Location>?) {
		Log.debug("Requesting a random location")
	  returnLocationOrError(this.locationService.getRandomLocation(), responseObserver)
	  responseObserver?.onCompleted()
  }

	@Blocking
	override fun getLocationByName(request: GetLocationRequest?, responseObserver: StreamObserver<Location>?) {
		if (request != null) {
			Log.debug("Getting location ${request.name}")
			returnLocationOrError(this.locationService.getLocationByName(request.name), responseObserver)
		}

		responseObserver?.onCompleted()
	}

	@Blocking
	override fun getAllLocations(request: AllLocationsRequest?, responseObserver: StreamObserver<LocationsList>?) {
		val allLocations = this.locationService.getAllLocations()
		Log.debug("Got all locations: $allLocations")

		responseObserver?.onNext(
			LocationsList.newBuilder()
				.addAllLocations(allLocations.map(LocationMapper::toGrpcLocation))
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

	private fun returnLocationOrError(location: io.quarkus.sample.superheroes.location.Location?, responseObserver: StreamObserver<Location>?) {
		if (location != null) {
			Log.info("Location found: $location")
			responseObserver?.onNext(LocationMapper.toGrpcLocation(location))
    }
    else {
			Log.info("No location was found")
			responseObserver?.onError(
				Status.NOT_FOUND
					.withDescription("A location was not found")
					.asException()
			)
    }
	}
}
