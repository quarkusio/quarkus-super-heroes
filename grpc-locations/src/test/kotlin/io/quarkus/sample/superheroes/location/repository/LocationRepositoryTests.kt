package io.quarkus.sample.superheroes.location.repository

import jakarta.inject.Inject
import io.quarkus.sample.superheroes.location.Location
import io.quarkus.sample.superheroes.location.LocationType
import io.quarkus.test.TestTransaction
import io.quarkus.test.junit.QuarkusTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@QuarkusTest
@TestTransaction
class LocationRepositoryTests {
	companion object {
		private const val DEFAULT_NAME = "Gotham City"
		private const val DEFAULT_DESCRIPTION = "Where Batman lives"
		private const val DEFAULT_PICTURE = "gotham_city.png"
		private val DEFAULT_TYPE = LocationType.PLANET

		private fun createDefaultLocation() : Location {
			val location = Location()
			location.name = DEFAULT_NAME
			location.description = DEFAULT_DESCRIPTION
			location.picture = DEFAULT_PICTURE
			location.type = DEFAULT_TYPE

			return location
		}
	}

  @Inject
  lateinit var locationRepository: LocationRepository

	@Test
	fun `find a random location when one does not exist`() {
		this.locationRepository.deleteAll()

		assertThat(this.locationRepository.count())
			.isZero()

		assertThat(this.locationRepository.findRandom())
			.isNull()
	}

	@Test
	fun `find a random location`() {
		val location = createDefaultLocation()

		this.locationRepository.deleteAll()
		this.locationRepository.persist(location)

		assertThat(this.locationRepository.count())
			.isOne()

		val l = this.locationRepository.findRandom()

		assertThat(l)
			.isNotNull()
			.usingRecursiveComparison()
			.isEqualTo(location)

		assertThat(l?.id)
			.isNotNull()
			.isPositive()
	}

	@Test
	fun `find an unknown location by name`() {
		this.locationRepository.deleteAll()
		assertThat(this.locationRepository.findByName("Raleigh"))
			.isNull()
	}

	@Test
	fun `find a location by name`() {
		val location = createDefaultLocation()
		this.locationRepository.deleteAll()
		this.locationRepository.persist(location)

		assertThat(this.locationRepository.findByName(location.name))
			.isNotNull
			.usingRecursiveComparison()
			.isEqualTo(location)
	}
}
