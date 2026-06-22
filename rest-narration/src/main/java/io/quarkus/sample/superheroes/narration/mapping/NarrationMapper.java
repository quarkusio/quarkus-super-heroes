package io.quarkus.sample.superheroes.narration.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.ImageGenerationRequest;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public interface NarrationMapper {
	Fight toFight(io.quarkus.sample.superheroes.narration.api.model.Fight apiModel);

	Fight.FightLocation toFightLocation(io.quarkus.sample.superheroes.narration.api.model.FightLocation apiModel);

	io.quarkus.sample.superheroes.narration.api.model.FightImage toApiFightImage(FightImage fightImage);

	ImageGenerationRequest toImageGenerationRequest(io.quarkus.sample.superheroes.narration.api.model.ImageGenerationRequest apiModel);
}
