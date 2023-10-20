package io.quarkus.sample.superheroes.location.service

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import io.quarkus.sample.superheroes.location.repository.LocationRepository

@ApplicationScoped
class LocationService(private val locationRepository: LocationRepository) {
	@WithSpan("LocationService.getRandomLocation")
  fun getRandomLocation() = this.locationRepository.findRandom()

	@WithSpan("LocationService.getAllLocations")
	fun getAllLocations() = this.locationRepository.listAll()

	@WithSpan("LocationService.deleteAllLocations")
	@Transactional(REQUIRED)
	fun deleteAllLocations() {
		this.locationRepository.deleteAll()
	}
}
