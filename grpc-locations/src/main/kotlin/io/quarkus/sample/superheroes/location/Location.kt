package io.quarkus.sample.superheroes.location

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "locations")
class Location {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  @SequenceGenerator(name = "locations_seq", initialValue = 1, allocationSize = 1)
  var id: Long? = null

	@NotNull
	@Size(min = 3, max = 50)
	@Column(nullable = false, length = 50)
  lateinit var name: String
  lateinit var description: String
  lateinit var picture: String

	override fun toString(): String {
		return "Location(id=$id, name='$name', description='$description', picture='$picture')"
	}
}
