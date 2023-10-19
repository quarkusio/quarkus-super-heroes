package io.quarkus.sample.superheroes.location.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.sample.superheroes.location.Location
import jakarta.enterprise.context.ApplicationScoped
import kotlin.random.Random

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
}
