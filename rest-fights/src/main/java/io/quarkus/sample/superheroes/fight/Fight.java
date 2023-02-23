package io.quarkus.sample.superheroes.fight;

import java.time.Instant;
import java.util.Objects;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;

/**
 * Mongo entity class for a Fight. Re-used in the API layer
 */
@MongoEntity(collection = "Fights")
@Schema(description = "Each fight has a winner and a loser")
public class Fight extends ReactivePanacheMongoEntity {
	@NotNull
	public Instant fightDate;

	@NotEmpty
	public String winnerName;

	@NotNull
	public Integer winnerLevel;

	@NotEmpty
	public String winnerPicture;

	@NotEmpty
	public String loserName;

	@NotNull
	public Integer loserLevel;

	@NotEmpty
	public String loserPicture;

	@NotEmpty
	public String winnerTeam;

	@NotEmpty
	public String loserTeam;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Fight fight = (Fight) o;
		return this.id == fight.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public String toString() {
		return "Fight{" +
			"fightDate=" + this.fightDate +
			", id=" + this.id +
			", winnerName='" + this.winnerName + '\'' +
			", winnerLevel=" + this.winnerLevel +
			", winnerPicture='" + this.winnerPicture + '\'' +
			", loserName='" + this.loserName + '\'' +
			", loserLevel=" + this.loserLevel +
			", loserPicture='" + this.loserPicture + '\'' +
			", winnerTeam='" + this.winnerTeam + '\'' +
			", loserTeam='" + this.loserTeam + '\'' +
			'}';
	}
}
