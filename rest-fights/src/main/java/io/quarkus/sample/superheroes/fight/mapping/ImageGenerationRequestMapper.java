package io.quarkus.sample.superheroes.fight.mapping;

import static org.mapstruct.MappingConstants.ComponentModel.JAKARTA_CDI;

import org.mapstruct.Mapper;

import io.quarkus.sample.superheroes.fight.ImageGenerationRequest;
import io.quarkus.sample.superheroes.fight.client.NarrationImageGenerationRequest;

@Mapper(componentModel = JAKARTA_CDI)
public interface ImageGenerationRequestMapper {
  NarrationImageGenerationRequest toClientRequest(ImageGenerationRequest request);
}
