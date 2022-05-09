package io.quarkus.sample.superheroes.statistics.domain;

import java.util.StringJoiner;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Data class for a team score
 * <p>
 *   The {@link RegisterForReflection @RegisterForReflection} annotation instructs the native compilation to allow reflection access to the class. Without it, the serialization/deserialization would not work when running the native executable.
 * </p>
 */
@RegisterForReflection
public class TeamScore {
  private final int heroWins;
  private final int villainWins;

  @JsonCreator
  public TeamScore(int heroWins, int villainWins) {
    this.heroWins = heroWins;
    this.villainWins = villainWins;
  }

  public int getHeroWins() {
    return heroWins;
  }

  public int getVillainWins() {
    return villainWins;
  }

  public int getNumberOfFights() {
    return getHeroWins() + getVillainWins();
  }

  public double getHeroWinRatio() {
    return ((double) this.heroWins / getNumberOfFights());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", TeamScore.class.getSimpleName() + "[", "]")
      .add("heroWins=" + heroWins)
      .add("villainWins=" + villainWins)
      .add("numberOfFights=" + getNumberOfFights())
      .add("heroWinRatio=" + getHeroWinRatio())
      .toString();
  }
}
