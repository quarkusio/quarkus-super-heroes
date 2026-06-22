package io.quarkus.sample.superheroes.villain.mapping;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.sample.superheroes.villain.Villain;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface VillainMapper {
	io.quarkus.sample.superheroes.villain.api.model.Villain toApiModel(Villain entity);

	List<io.quarkus.sample.superheroes.villain.api.model.Villain> toApiModelList(List<Villain> entities);

	Villain toEntity(io.quarkus.sample.superheroes.villain.api.model.Villain apiModel);

	List<Villain> toEntityList(List<io.quarkus.sample.superheroes.villain.api.model.Villain> apiModels);

	Villain toEntity(io.quarkus.sample.superheroes.villain.api.model.VillainPatch patch);
}
