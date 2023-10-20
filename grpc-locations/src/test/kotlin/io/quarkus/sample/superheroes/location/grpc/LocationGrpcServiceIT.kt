package io.quarkus.sample.superheroes.location.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import java.util.concurrent.TimeUnit
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.grpc.LocationsGrpc.LocationsBlockingStub
import io.quarkus.test.junit.QuarkusIntegrationTest
import org.assertj.core.api.Assertions.assertThat
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
		private var NB_LOCATIONS = 1
		private const val DEFAULT_ORDER = 0
		private val deadlineInterceptor =
			object : ClientInterceptor {
				override fun <ReqT : Any?, RespT : Any?> interceptCall(method: MethodDescriptor<ReqT, RespT>?, callOptions: CallOptions?, next: Channel?) =
					next?.newCall(method, callOptions?.withDeadlineAfter(10, TimeUnit.SECONDS))!!
			}

		private lateinit var channel: ManagedChannel
		private lateinit var locationsGrpcService : LocationsBlockingStub

		private fun createDefaultLocation() : Location {
			val location = Location()
			location.id = DEFAULT_ID
			location.name = DEFAULT_NAME
			location.description = DEFAULT_DESCRIPTION
			location.picture = DEFAULT_PICTURE

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
				.withInterceptors(deadlineInterceptor)
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
	}

	@Test
	@Order(DEFAULT_ORDER)
	fun `Get all locations`() {
		val location = createDefaultLocation()

		assertThat(locationsGrpcService.getAllLocations(AllLocationsRequest.newBuilder().build())?.locationsList)
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
}