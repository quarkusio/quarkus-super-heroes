package io.quarkus.sample.superheroes.location

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Entity
@Table(name = "locations")
class Location {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null

	@NotNull
	@Size(min = 3, max = 50)
	@Column(nullable = false, length = 50, unique = true)
	lateinit var name: String

	@Column(length = 5000)
	var description: String? = null
	var picture: String? = null

	@Enumerated(STRING)
	@Column(nullable = false)
	lateinit var type: LocationType

	override fun toString(): String {
		return "Location(id=$id, name='$name', description='$description', picture='$picture', type='$type')"
	}
}
