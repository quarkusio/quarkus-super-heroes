package io.quarkus.sample.superheroes.hero.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Hero data exposed on the Graftcode callable surface (no JPA or HTTP types).
 */
public class HeroDto {

  private Long id;

  @NotNull
  @Size(min = 3, max = 50)
  private String name;

  private String otherName;

  @NotNull
  @Positive
  private Integer level;

  private String picture;

  private String powers;

  public Long getId() {
    return id;
  }

  public HeroDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public HeroDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getOtherName() {
    return otherName;
  }

  public HeroDto setOtherName(String otherName) {
    this.otherName = otherName;
    return this;
  }

  public Integer getLevel() {
    return level;
  }

  public HeroDto setLevel(Integer level) {
    this.level = level;
    return this;
  }

  public String getPicture() {
    return picture;
  }

  public HeroDto setPicture(String picture) {
    this.picture = picture;
    return this;
  }

  public String getPowers() {
    return powers;
  }

  public HeroDto setPowers(String powers) {
    this.powers = powers;
    return this;
  }
}
