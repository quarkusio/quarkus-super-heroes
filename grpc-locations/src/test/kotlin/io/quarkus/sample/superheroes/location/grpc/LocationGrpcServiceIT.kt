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
		private const val DEFAULT_DESCRIPTION = "Dark city where Batman lives"
		private const val DEFAULT_PICTURE = "gotham_city.png"
		private val DEFAULT_TYPE = LocationType.CITY
		private var NB_LOCATIONS = 1
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
			assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.newBuilder().build())?.locationsList)
				.isNotNull
				.hasSize(expected)
		}
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Hello endpoint`() {
		assertThat(locationsGrpcService.hello(HelloRequest.newBuilder().build()))
			.isNotNull
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `starts with 1 location`() {
		verifyNumberOfLocations()
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Get a random location`() {
		var randomLocation = createDefaultLocation()

		assertThat(locationsGrpcService.getRandomLocation(RandomLocationRequest.newBuilder().build()))
			.isNotNull
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(randomLocation))
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Get all locations`() {
		val location = createDefaultLocation()

		assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.newBuilder().build())?.locationsList)
			.isNotNull
			.singleElement()
			.usingRecursiveComparison().ignoringFields("memoizedIsInitialized")
			.isEqualTo(LocationMapper.toGrpcLocation(location))
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
		locationsGrpcService.deleteAllLocations(DeleteAllLocationsRequest.newBuilder().build())
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
		assertThatThrownBy { locationsGrpcService.getRandomLocation(RandomLocationRequest.newBuilder().build()) }
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
}