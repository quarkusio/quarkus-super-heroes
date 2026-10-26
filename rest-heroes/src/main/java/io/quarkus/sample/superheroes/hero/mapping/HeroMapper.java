package io.quarkus.sample.superheroes.hero.mapping;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.sample.superheroes.hero.Hero;
import io.quarkus.sample.superheroes.hero.model.HeroDto;
import io.quarkus.sample.superheroes.hero.model.HeroPatchDto;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface HeroMapper {
  HeroDto toDto(Hero entity);

  List<HeroDto> toDtoList(List<Hero> entities);

  Hero toEntity(HeroDto dto);

  List<Hero> toEntityList(List<HeroDto> dtos);

  Hero toEntity(HeroPatchDto patch);
}
