package io.quarkus.sample.superheroes.hero.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.model.HeroDto;
import io.quarkus.sample.superheroes.hero.model.HeroPatchDto;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HeroMapperTests {
  private static final Long DEFAULT_ID = 1L;
  private static final String DEFAULT_NAME = "Luke Skywalker";
  private static final String DEFAULT_OTHER_NAME = "Jedi Knight";
  private static final String DEFAULT_PICTURE = "luke-skywalker.jpg";
  private static final String DEFAULT_POWERS = "Uses light sabre, The force";
  private static final int DEFAULT_LEVEL = 10;

  @Inject
  HeroMapper heroMapper;

  @Test
  void toDto() {
    var entity = createEntity();
    var dto = this.heroMapper.toDto(entity);

    assertThat(dto)
      .isNotNull()
      .extracting(HeroDto::getId, HeroDto::getName, HeroDto::getOtherName, HeroDto::getLevel, HeroDto::getPicture, HeroDto::getPowers)
      .containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
  }

  @Test
  void toDtoNull() {
    assertThat(this.heroMapper.toDto(null))
      .isNull();
  }

  @Test
  void toDtoList() {
    var entities = List.of(createEntity());
    var dtos = this.heroMapper.toDtoList(entities);

    assertThat(dtos)
      .isNotNull()
      .singleElement()
      .extracting(HeroDto::getId, HeroDto::getName, HeroDto::getLevel)
      .containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
  }

  @Test
  void toEntity() {
    var dto = new HeroDto()
      .setId(DEFAULT_ID)
      .setName(DEFAULT_NAME)
      .setOtherName(DEFAULT_OTHER_NAME)
      .setLevel(DEFAULT_LEVEL)
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS);

    var entity = this.heroMapper.toEntity(dto);

    assertThat(entity)
      .isNotNull()
      .extracting(Hero::getId, Hero::getName, Hero::getOtherName, Hero::getLevel, Hero::getPicture, Hero::getPowers)
      .containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
  }

  @Test
  void toEntityNull() {
    assertThat(this.heroMapper.toEntity((HeroDto) null))
      .isNull();
  }

  @Test
  void toEntityList() {
    var dtos = List.of(
      new HeroDto()
        .setId(DEFAULT_ID)
        .setName(DEFAULT_NAME)
        .setLevel(DEFAULT_LEVEL)
    );

    var entities = this.heroMapper.toEntityList(dtos);

    assertThat(entities)
      .isNotNull()
      .singleElement()
      .extracting(Hero::getId, Hero::getName, Hero::getLevel)
      .containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
  }

  @Test
  void toEntityFromPatch() {
    var patch = new HeroPatchDto()
      .setId(DEFAULT_ID)
      .setName(DEFAULT_NAME)
      .setOtherName(DEFAULT_OTHER_NAME)
      .setLevel(DEFAULT_LEVEL)
      .setPicture(DEFAULT_PICTURE)
      .setPowers(DEFAULT_POWERS);

    var entity = this.heroMapper.toEntity(patch);

    assertThat(entity)
      .isNotNull()
      .extracting(Hero::getId, Hero::getName, Hero::getOtherName, Hero::getLevel, Hero::getPicture, Hero::getPowers)
      .containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
  }

  @Test
  void toEntityFromPatchNull() {
    assertThat(this.heroMapper.toEntity((HeroPatchDto) null))
      .isNull();
  }

  private Hero createEntity() {
    var hero = new Hero();
    hero.setId(DEFAULT_ID);
    hero.setName(DEFAULT_NAME);
    hero.setOtherName(DEFAULT_OTHER_NAME);
    hero.setLevel(DEFAULT_LEVEL);
    hero.setPicture(DEFAULT_PICTURE);
    hero.setPowers(DEFAULT_POWERS);
    return hero;
  }
}
