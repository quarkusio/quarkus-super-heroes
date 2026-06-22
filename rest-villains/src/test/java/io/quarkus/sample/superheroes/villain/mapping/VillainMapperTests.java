package io.quarkus.sample.superheroes.villain.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.villain.Villain;
import io.quarkus.sample.superheroes.villain.api.model.VillainPatch;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class VillainMapperTests {
	private static final Long DEFAULT_ID = 1L;
	private static final String DEFAULT_NAME = "Super Chocolatine";
	private static final String DEFAULT_OTHER_NAME = "Super Chocolatine chocolate in";
	private static final String DEFAULT_PICTURE = "super_chocolatine.png";
	private static final String DEFAULT_POWERS = "does not eat pain au chocolat";
	private static final int DEFAULT_LEVEL = 42;

	@Inject
	VillainMapper villainMapper;

	@Test
	void toApiModel() {
		var entity = createEntity();
		var apiModel = this.villainMapper.toApiModel(entity);

		assertThat(apiModel)
			.isNotNull()
			.extracting(
				io.quarkus.sample.superheroes.villain.api.model.Villain::getId,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getName,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getOtherName,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getLevel,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getPicture,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getPowers
			)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toApiModelNull() {
		assertThat(this.villainMapper.toApiModel(null))
			.isNull();
	}

	@Test
	void toApiModelList() {
		var entities = List.of(createEntity());
		var apiModels = this.villainMapper.toApiModelList(entities);

		assertThat(apiModels)
			.isNotNull()
			.singleElement()
			.extracting(
				io.quarkus.sample.superheroes.villain.api.model.Villain::getId,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getName,
				io.quarkus.sample.superheroes.villain.api.model.Villain::getLevel
			)
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
	}

	@Test
	void toEntity() {
		var apiModel = new io.quarkus.sample.superheroes.villain.api.model.Villain()
			.id(DEFAULT_ID)
			.name(DEFAULT_NAME)
			.otherName(DEFAULT_OTHER_NAME)
			.level(DEFAULT_LEVEL)
			.picture(DEFAULT_PICTURE)
			.powers(DEFAULT_POWERS);

		var entity = this.villainMapper.toEntity(apiModel);

		assertThat(entity)
			.isNotNull()
			.extracting("id", "name", "otherName", "level", "picture", "powers")
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toEntityNull() {
		assertThat(this.villainMapper.toEntity((io.quarkus.sample.superheroes.villain.api.model.Villain) null))
			.isNull();
	}

	@Test
	void toEntityList() {
		var apiModels = List.of(
			new io.quarkus.sample.superheroes.villain.api.model.Villain()
				.id(DEFAULT_ID)
				.name(DEFAULT_NAME)
				.level(DEFAULT_LEVEL)
		);

		var entities = this.villainMapper.toEntityList(apiModels);

		assertThat(entities)
			.isNotNull()
			.singleElement()
			.extracting("id", "name", "level")
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_LEVEL);
	}

	@Test
	void toEntityFromPatch() {
		var patch = new VillainPatch()
			.id(DEFAULT_ID)
			.name(DEFAULT_NAME)
			.otherName(DEFAULT_OTHER_NAME)
			.level(DEFAULT_LEVEL)
			.picture(DEFAULT_PICTURE)
			.powers(DEFAULT_POWERS);

		var entity = this.villainMapper.toEntity(patch);

		assertThat(entity)
			.isNotNull()
			.extracting("id", "name", "otherName", "level", "picture", "powers")
			.containsExactly(DEFAULT_ID, DEFAULT_NAME, DEFAULT_OTHER_NAME, DEFAULT_LEVEL, DEFAULT_PICTURE, DEFAULT_POWERS);
	}

	@Test
	void toEntityFromPatchNull() {
		assertThat(this.villainMapper.toEntity((VillainPatch) null))
			.isNull();
	}

	private Villain createEntity() {
		var villain = new Villain();
		villain.id = DEFAULT_ID;
		villain.name = DEFAULT_NAME;
		villain.otherName = DEFAULT_OTHER_NAME;
		villain.level = DEFAULT_LEVEL;
		villain.picture = DEFAULT_PICTURE;
		villain.powers = DEFAULT_POWERS;
		return villain;
	}
}
