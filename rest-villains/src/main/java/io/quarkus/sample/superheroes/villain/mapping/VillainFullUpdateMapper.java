package io.quarkus.sample.superheroes.villain.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import io.quarkus.sample.superheroes.villain.Villain;

@Mapper(componentModel = "cdi")
public interface VillainFullUpdateMapper {
	@Mapping(target = "id", ignore = true)
	void mapFullUpdate(Villain input, @MappingTarget Villain target);
}
