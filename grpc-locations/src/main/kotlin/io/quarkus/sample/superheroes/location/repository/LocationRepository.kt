package io.quarkus.sample.superheroes.location.repository

import jakarta.enterprise.context.ApplicationScoped
import kotlin.random.Random
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.sample.superheroes.location.Location

@ApplicationScoped
class LocationRepository : PanacheRepository<Location> {
  fun findRandom(): Location? {
    val countLocations = count()

    if (countLocations > 0) {
      val randomLocation = Random.nextInt(countLocations.toInt())
      return findAll().page(randomLocation, 1).firstResult()
    }

    return null
  }

	fun findByName(name: String) = find("name", name).firstResult()
}
