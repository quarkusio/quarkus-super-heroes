package io.quarkus.sample.superheroes.location.grpc

import io.grpc.Status.Code
import io.grpc.StatusRuntimeException
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import io.quarkus.grpc.GrpcClient
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.LocationType
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsBlockingStub
import io.quarkus.sample.superheroes.location.mapping.LocationMapper
import io.quarkus.sample.superheroes.location.service.LocationService
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
		private val DEFAULT_TYPE = LocationType.CITY

		private fun createDefaultLocation() : Location {
			val location = Location()
			location.id = DEFAULT_ID
			location.name = DEFAULT_NAME
			location.description = DEFAULT_DESCRIPTION
			location.picture = DEFAULT_PICTURE
			location.type = DEFAULT_TYPE

			return location
		}
	}

	@InjectMock
	lateinit var locationService: LocationService

	@GrpcClient
	lateinit var locationsGrpcService : LocationsBlockingStub

	@Test
	fun `Hello endpoint`() {
		assertThat(this.locationsGrpcService.hello(HelloRequest.getDefaultInstance()))
			.isNotNull

		verify(exactly = 0, verifyBlock = { locationService.getRandomLocation() })
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get a random location`() {
		var randomLocation = createDefaultLocation()

		every { locationService.getRandomLocation() } returns randomLocation

		assertThat(this.locationsGrpcService.getRandomLocation(RandomLocationRequest.getDefaultInstance()))
			.isNotNull
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(randomLocation))

		verify { locationService.getRandomLocation() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get a random location when one does not exist`() {
		every { locationService.getRandomLocation() } returns null

		assertThatThrownBy { this.locationsGrpcService.getRandomLocation(RandomLocationRequest.getDefaultInstance()) }
			.isNotNull()
			.isInstanceOf(StatusRuntimeException::class.java)
			.hasMessage("${Code.NOT_FOUND}: A location was not found")
			.extracting { (it as StatusRuntimeException).status.code }
			.isEqualTo(Code.NOT_FOUND)

		verify { locationService.getRandomLocation() }
		confirmVerified(this.locationService)
	}
	
	@Test
	fun `Get all locations`() {
		val location = createDefaultLocation()
		every { locationService.getAllLocations() } returns listOf(location)

		assertThat(this.locationsGrpcService.getAllLocations(AllLocationsRequest.getDefaultInstance())?.locationsList)
			.isNotNull
			.singleElement()
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(location))

		verify { locationService.getAllLocations() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `Get all locations when none are found`() {
		every { locationService.getAllLocations() } returns emptyList()

		assertThat(this.locationsGrpcService.getAllLocations(AllLocationsRequest.getDefaultInstance())?.locationsList)
			.isNotNull
			.isEmpty()

		verify { locationService.getAllLocations() }
		confirmVerified(this.locationService)
	}

	@Test
	fun `replace all locations`() {
		justRun { locationService.replaceAllLocations(any()) }

		assertThat(this.locationsGrpcService.replaceAllLocations(LocationsList.getDefaultInstance()))
			.isNotNull

		verify { locationService.replaceAllLocations(any()) }
		confirmVerified(this.locationService)
	}

	@Test
	fun `get location by name`() {
		val location = createDefaultLocation()
		every { locationService.getLocationByName(location.name) } returns location

		assertThat(this.locationsGrpcService.getLocationByName(GetLocationRequest.newBuilder().setName(location.name).build()))
			.isNotNull
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(location))

		verify { locationService.getLocationByName(location.name) }
		confirmVerified(this.locationService)
	}

	@Test
	fun `get location by name when not found`() {
		every { locationService.getLocationByName(DEFAULT_NAME) } returns null

		assertThatThrownBy { this.locationsGrpcService.getLocationByName(GetLocationRequest.newBuilder().setName(DEFAULT_NAME).build()) }
			.isNotNull()
			.isInstanceOf(StatusRuntimeException::class.java)
			.hasMessage("${Code.NOT_FOUND}: A location was not found")
			.extracting { (it as StatusRuntimeException).status.code }
			.isEqualTo(Code.NOT_FOUND)

		verify { locationService.getLocationByName(DEFAULT_NAME) }
		confirmVerified(this.locationService)
	}
}
