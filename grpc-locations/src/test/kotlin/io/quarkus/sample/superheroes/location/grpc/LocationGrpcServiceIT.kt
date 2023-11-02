package io.quarkus.sample.superheroes.location.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status.Code
import io.grpc.StatusRuntimeException
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.LocationType
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsBlockingStub
import io.quarkus.sample.superheroes.location.mapping.LocationMapper
import io.quarkus.test.junit.QuarkusIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.microprofile.config.ConfigProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@QuarkusIntegrationTest
@TestMethodOrder(OrderAnnotation::class)
class LocationGrpcServiceIT {
	companion object {
		private const val DEFAULT_ID = 1L
		private const val DEFAULT_NAME = "Gotham City"
		private const val DEFAULT_DESCRIPTION = "An American city rife with corruption and crime, the home of its iconic protector Batman."
		private const val DEFAULT_PICTURE = "https://raw.githubusercontent.com/quarkusio/quarkus-super-heroes/characterdata/images/locations/gotham_city.jpg"
		private val DEFAULT_TYPE = LocationType.CITY
		private var NB_LOCATIONS = 30
		private const val DEFAULT_ORDER = 0

		private lateinit var channel: ManagedChannel
		private lateinit var locationsGrpcService : LocationsBlockingStub

		private fun createDefaultLocation() : Location {
			val location = Location()
			location.id = DEFAULT_ID
			location.name = DEFAULT_NAME
			location.description = DEFAULT_DESCRIPTION
			location.picture = DEFAULT_PICTURE
			location.type = DEFAULT_TYPE

			return location
		}

		private fun createDefaultGrpcLocation() = LocationMapper.toGrpcLocation(createDefaultLocation())

		@BeforeAll
		@JvmStatic
		internal fun beforeAll() {
			val port = ConfigProvider.getConfig().getValue("quarkus.http.test-port", Integer::class.java)
			channel = ManagedChannelBuilder.forAddress("localhost", port.toInt())
				.usePlaintext()
				.build()

			locationsGrpcService = LocationsGrpc
				.newBlockingStub(channel)
				.withInterceptors(DeadlineInterceptor())
		}

		@AfterAll
		@JvmStatic
		internal fun afterAll() {
			channel.shutdownNow()
		}

		fun verifyNumberOfLocations(expected: Int = NB_LOCATIONS) {
			assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.getDefaultInstance())?.locationsList)
				.isNotNull
				.hasSize(expected)
		}
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Hello endpoint`() {
		assertThat(locationsGrpcService.hello(HelloRequest.getDefaultInstance()))
			.isNotNull
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `starts with 4 locations`() {
		verifyNumberOfLocations()
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Get a random location`() {
		assertThat(locationsGrpcService.getRandomLocation(RandomLocationRequest.getDefaultInstance()))
			.isNotNull
			.hasNoNullFieldsOrProperties()
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Get all locations`() {
		assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.getDefaultInstance())?.locationsList)
			.isNotNull
			.hasSize(NB_LOCATIONS)
			.doesNotContainNull()
			.allSatisfy { location -> assertThat(location).hasNoNullFieldsOrProperties() }
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `get location by name`() {
		val location = createDefaultLocation()

		assertThat(locationsGrpcService.getLocationByName(GetLocationRequest.newBuilder().setName(location.name).build()))
			.isNotNull
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(location))
	}

	@Test
	@Order(DEFAULT_ORDER + 1)
	fun `delete all locations`() {
		locationsGrpcService.deleteAllLocations(DeleteAllLocationsRequest.getDefaultInstance())
		verifyNumberOfLocations(0)
	}

	@Test
	@Order(DEFAULT_ORDER + 2)
	fun `Get all locations when there arent any`() {
		verifyNumberOfLocations(0)
	}

	@Test
	@Order(DEFAULT_ORDER + 2)
	fun `Get a random location when one does not exist`() {
		assertThatThrownBy { locationsGrpcService.getRandomLocation(RandomLocationRequest.getDefaultInstance()) }
			.isNotNull()
			.isInstanceOf(StatusRuntimeException::class.java)
			.hasMessage("${Code.NOT_FOUND}: A location was not found")
			.extracting { (it as StatusRuntimeException).status.code }
			.isEqualTo(Code.NOT_FOUND)
	}

	@Test
	@Order(DEFAULT_ORDER + 2)
	fun `get location by name when not found`() {
		assertThatThrownBy { locationsGrpcService.getLocationByName(GetLocationRequest.newBuilder().setName(DEFAULT_NAME).build()) }
			.isNotNull()
			.isInstanceOf(StatusRuntimeException::class.java)
			.hasMessage("${Code.NOT_FOUND}: A location was not found")
			.extracting { (it as StatusRuntimeException).status.code }
			.isEqualTo(Code.NOT_FOUND)
	}

	@Test
	@Order(DEFAULT_ORDER + 3)
	fun `replace all locations`() {
		locationsGrpcService.replaceAllLocations(LocationsList.newBuilder().addAllLocations(listOf(createDefaultGrpcLocation())).build())
		verifyNumberOfLocations(1)
	}

	@Test
	@Order(DEFAULT_ORDER + 4)
	fun `Get all locations after replacing`() {
		assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.getDefaultInstance())?.locationsList)
			.isNotNull
			.singleElement()
			.isEqualTo(createDefaultGrpcLocation())
	}
}