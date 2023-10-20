package io.quarkus.sample.superheroes.location.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import io.quarkiverse.test.junit.mockk.InjectMock
import jakarta.inject.Inject
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.repository.LocationRepository
import io.quarkus.test.junit.QuarkusTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@QuarkusTest
class LocationServiceTests {
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
	lateinit var locationRepository: LocationRepository

	@Inject
	lateinit var locationService: LocationService

	@Test
	fun `find a random location`() {
		val defaultLocation = createDefaultLocation()

		every { locationRepository.findRandom() } returns defaultLocation

		assertThat(this.locationService.getRandomLocation())
			.isNotNull
			.usingRecursiveComparison()
			.isEqualTo(defaultLocation)

		verify { locationRepository.findRandom() }
		confirmVerified(this.locationRepository)
	}

	@Test
	fun `find a random location when one does not exist`() {
		every { locationRepository.findRandom() } returns null

		assertThat(this.locationService.getRandomLocation())
			.isNull()

		verify { locationRepository.findRandom() }
		confirmVerified(this.locationRepository)
	}

	@Test
	fun `find all locations`() {
		val defaultLocation = createDefaultLocation()

		every { locationRepository.listAll() } returns listOf(defaultLocation)

		assertThat(this.locationService.getAllLocations())
			.isNotNull
			.singleElement()
			.usingRecursiveComparison()
			.isEqualTo(defaultLocation)

		verify { locationRepository.listAll() }
		confirmVerified(this.locationRepository)
	}

	@Test
	fun `find all locations when there arent any`() {
		every { locationRepository.listAll() } returns emptyList()

		assertThat(this.locationService.getAllLocations())
			.isNotNull
			.isEmpty()

		verify { locationRepository.listAll() }
		confirmVerified(this.locationRepository)
	}
}