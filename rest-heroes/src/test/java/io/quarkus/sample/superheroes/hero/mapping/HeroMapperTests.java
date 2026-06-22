package io.quarkus.sample.superheroes.hero.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.api.model.HeroPatch;
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
	void toApiModel() {
		var entity = createEntity();
		var apiModel = this.heroMapper.toApiModel(entity);

		assertThat(apiModel)
			.isNotNull()
			.extracting(
				io.quarkus.sample.superheroes.hero.api.model.Hero::getId,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getName,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getOtherName,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getLevel,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getPicture,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getPowers
			)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toApiModelNull() {
		assertThat(this.heroMapper.toApiModel(null))
			.isNull();
	}

	@Test
	void toApiModelList() {
		var entities = List.of(createEntity());
		var apiModels = this.heroMapper.toApiModelList(entities);

		assertThat(apiModels)
			.isNotNull()
			.singleElement()
			.extracting(
				io.quarkus.sample.superheroes.hero.api.model.Hero::getId,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getName,
				io.quarkus.sample.superheroes.hero.api.model.Hero::getLevel
			)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
	}

	@Test
	void toEntity() {
		var apiModel = new io.quarkus.sample.superheroes.hero.api.model.Hero()
			.id(DEFAULT_ID)
			.name(DEFAULT_NAME)
			.otherName(DEFAULT_OTHER_NAME)
			.level(DEFAULT_LEVEL)
			.picture(DEFAULT_PICTURE)
			.powers(DEFAULT_POWERS);

		var entity = this.heroMapper.toEntity(apiModel);

		assertThat(entity)
			.isNotNull()
			.extracting(Hero::getId, Hero::getName, Hero::getOtherName, Hero::getLevel, Hero::getPicture, Hero::getPowers)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toEntityNull() {
		assertThat(this.heroMapper.toEntity((io.quarkus.sample.superheroes.hero.api.model.Hero) null))
			.isNull();
	}

	@Test
	void toEntityList() {
		var apiModels = List.of(
			new io.quarkus.sample.superheroes.hero.api.model.Hero()
				.id(DEFAULT_ID)
				.name(DEFAULT_NAME)
				.level(DEFAULT_LEVEL)
		);

		var entities = this.heroMapper.toEntityList(apiModels);

		assertThat(entities)
			.isNotNull()
			.singleElement()
			.extracting(Hero::getId, Hero::getName, Hero::getLevel)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
	}

	@Test
	void toEntityFromPatch() {
		var patch = new HeroPatch()
			.id(DEFAULT_ID)
			.name(DEFAULT_NAME)
			.otherName(DEFAULT_OTHER_NAME)
			.level(DEFAULT_LEVEL)
			.picture(DEFAULT_PICTURE)
			.powers(DEFAULT_POWERS);

		var entity = this.heroMapper.toEntity(patch);

		assertThat(entity)
			.isNotNull()
			.extracting(Hero::getId, Hero::getName, Hero::getOtherName, Hero::getLevel, Hero::getPicture, Hero::getPowers)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toEntityFromPatchNull() {
		assertThat(this.heroMapper.toEntity((HeroPatch) null))
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
