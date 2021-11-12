package io.quarkus.sample.superheroes.villain.mapping;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import io.quarkus.sample.superheroes.villain.Villain;

@Mapper(componentModel = "cdi", nullValuePropertyMappingStrategy = IGNORE)
public interface VillainPartialUpdateMapper {
	void mapPartialUpdate(Villain input, @MappingTarget Villain target);
}
