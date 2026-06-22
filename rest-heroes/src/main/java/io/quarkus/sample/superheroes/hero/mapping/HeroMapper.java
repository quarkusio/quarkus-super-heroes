package io.quarkus.sample.superheroes.hero.mapping;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.api.model.HeroPatch;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface HeroMapper {
	io.quarkus.sample.superheroes.hero.api.model.Hero toApiModel(Hero entity);

	List<io.quarkus.sample.superheroes.hero.api.model.Hero> toApiModelList(List<Hero> entities);

	Hero toEntity(io.quarkus.sample.superheroes.hero.api.model.Hero apiModel);

	List<Hero> toEntityList(List<io.quarkus.sample.superheroes.hero.api.model.Hero> apiModels);

	Hero toEntity(HeroPatch patch);
}
