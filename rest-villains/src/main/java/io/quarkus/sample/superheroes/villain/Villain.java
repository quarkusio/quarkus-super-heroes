package io.quarkus.sample.superheroes.villain;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * JPA entity class for a Villain. Re-used in the API layer.
 */
@Entity
public class Villain extends PanacheEntity {
	@NotNull
	@Size(min = 3, max = 50)
	public String name;

	public String otherName;

	@NotNull
	@Min(1)
	public Integer level;

	public String picture;

	@Column(columnDefinition = "TEXT")
	public String powers;

	public static Optional<Villain> findRandom() {
		long countVillains = count();

		if (countVillains > 0) {
			Random random = new Random();
			int randomVillain = random.nextInt((int) countVillains);
			return findAll().page(randomVillain, 1).firstResultOptional();
		}

		return Optional.empty();
	}

	@Override
	/* prettier-ignore */
	public String toString() {
		return (
			"Villain{" +
				"id=" + this.id +
				", name='" + this.name +
				'\'' +
				", otherName='" + this.otherName +
				'\'' +
				", level=" + this.level +
				", picture='" + this.picture +
				'\'' +
				", powers='" + this.powers +
				'\'' +
				'}'
		);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Villain villain = (Villain) o;
		return this.id.equals(villain.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}
}
