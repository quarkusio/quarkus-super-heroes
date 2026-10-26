package io.quarkus.sample.superheroes.hero.model;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Partial hero update for the Graftcode callable surface; all fields are optional.
 */
public class HeroPatchDto {

  private Long id;

  @Size(min = 3, max = 50)
  private String name;

  private String otherName;

  @Positive
  private Integer level;

  private String picture;

  private String powers;

  public Long getId() {
    return id;
  }

  public HeroPatchDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public HeroPatchDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getOtherName() {
    return otherName;
  }

  public HeroPatchDto setOtherName(String otherName) {
    this.otherName = otherName;
    return this;
  }

  public Integer getLevel() {
    return level;
  }

  public HeroPatchDto setLevel(Integer level) {
    this.level = level;
    return this;
  }

  public String getPicture() {
    return picture;
  }

  public HeroPatchDto setPicture(String picture) {
    this.picture = picture;
    return this;
  }

  public String getPowers() {
    return powers;
  }

  public HeroPatchDto setPowers(String powers) {
    this.powers = powers;
    return this;
  }
}
