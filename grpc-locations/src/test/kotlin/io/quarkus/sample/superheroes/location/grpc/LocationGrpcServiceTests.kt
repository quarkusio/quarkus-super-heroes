package io.quarkus.sample.superheroes.location.grpc

import io.grpc.Status.Code
import io.grpc.StatusRuntimeException
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import java.util.concurrent.TimeUnit
import io.quarkus.grpc.GrpcClient
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsBlockingStub
import io.quarkus.sample.superheroes.location.service.LocationService
import io.quarkus.test.junit.QuarkusTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@QuarkusTest
class LocationGrpcServiceTests {
	companion object {
		private const val DEFAULT_ID = 1L
		private const val DEFAULT_NAME = "Gotham City"
		private const val DEFAULT_DESCRIPTION = "Where Batman lives"
		private const val DEFAULT_PICTURE = "gotham_city.png"

		private fun createDefaultLocation() : Location {
			val location = Location()
			location.id = DEFAULT_ID
			location.name = DEFAULT_NAME
			location.description = DEFAULT_DESCRIPTION
			location.picture = DEFAULT_PICTURE

			return location
		}
	}

	@InjectMock
	lateinit var locationService: LocationService

	@GrpcClient
	lateinit var locationsGrpcService : LocationsBlockingStub

	private fun getLocationGrpcService() = locationsGrpcService.withDeadlineAfter(10, TimeUnit.SECONDS)

	@Test
	fun `Hello endpoint`() {
		assertThat(getLocationGrpcService().hello(HelloRequest.newBuilder().build()))
			.isNotNull

		verify(exactly = 0, verifyBlock = { locationService.getRandomLocation() })
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get a random location`() {
		var randomLocation = createDefaultLocation()

		every { locationService.getRandomLocation() } returns randomLocation

		assertThat(getLocationGrpcService().getRandomLocation(RandomLocationRequest.newBuilder().build()))
			.isNotNull
			.extracting(
				"name",
				"description",
				"picture"
			)
			.containsExactly(
				randomLocation.name,
				randomLocation.description,
				randomLocation.picture
			)

		verify { locationService.getRandomLocation() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get a random location when one does not exist`() {
		every { locationService.getRandomLocation() } returns null

		assertThatThrownBy { getLocationGrpcService().getRandomLocation(RandomLocationRequest.newBuilder().build()) }
			.isNotNull()
			.isInstanceOf(StatusRuntimeException::class.java)
			.hasMessage("${Code.NOT_FOUND}: A random location was not found")
			.extracting { (it as StatusRuntimeException).status.code }
			.isEqualTo(Code.NOT_FOUND)

		verify { locationService.getRandomLocation() }
		confirmVerified(this.locationService)
	}
	
	@Test
	fun `Get all locations`() {
		val location = createDefaultLocation()
		every { locationService.getAllLocations() } returns listOf(location)

		assertThat(getLocationGrpcService().getAllLocations(AllLocationsRequest.newBuilder().build())?.locationsList)
			.isNotNull
			.singleElement()
			.extracting(
					"name",
					"description",
					"picture"
				)
				.containsExactly(
					location.name,
					location.description,
					location.picture
				)
		
		verify { locationService.getAllLocations() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get all locations when none are found`() {
		every { locationService.getAllLocations() } returns emptyList()

		assertThat(getLocationGrpcService().getAllLocations(AllLocationsRequest.newBuilder().build())?.locationsList)
			.isNotNull
			.isEmpty()

		verify { locationService.getAllLocations() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `delete all locations`() {
		justRun { locationService.deleteAllLocations() }

		assertThat(getLocationGrpcService().deleteAllLocations(DeleteAllLocationsRequest.newBuilder().build()))
			.isNotNull

		verify { locationService.deleteAllLocations() }
		confirmVerified(this.locationService)
	}
}