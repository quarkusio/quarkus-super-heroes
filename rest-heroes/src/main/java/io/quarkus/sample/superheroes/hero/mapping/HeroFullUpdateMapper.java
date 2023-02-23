package io.quarkus.sample.superheroes.hero.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

import io.quarkus.sample.superheroes.hero.Hero;

/**
 * Mapper to map all fields on an input {@link Hero} onto a target {@link Hero}.
 */
@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface HeroFullUpdateMapper {
	/**
	 * Maps all fields except <code>id</code> from {@code input} onto {@code target}.
	 * @param input The input {@link Hero}
	 * @param target The target {@link Hero}
	 */
	@Mapping(target = "id", ignore = true)
	void mapFullUpdate(Hero input, @MappingTarget Hero target);
}
