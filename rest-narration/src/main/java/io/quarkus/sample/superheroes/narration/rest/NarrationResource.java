package io.quarkus.sample.superheroes.narration.rest;

import io.quarkus.logging.Log;

import io.quarkus.sample.superheroes.narration.api.model.Fight;
import io.quarkus.sample.superheroes.narration.api.model.FightImage;
import io.quarkus.sample.superheroes.narration.api.model.ImageGenerationRequest;
import io.quarkus.sample.superheroes.narration.mapping.NarrationMapper;
import io.quarkus.sample.superheroes.narration.service.ImageGenerationService;
import io.quarkus.sample.superheroes.narration.service.NarrationService;

public class NarrationResource implements io.quarkus.sample.superheroes.narration.api.resources.NarrationResource {
  private final NarrationService narrationService;
  private final ImageGenerationService imageGenerationService;
  private final NarrationMapper narrationMapper;

  public NarrationResource(NarrationService narrationService, ImageGenerationService imageGenerationService, NarrationMapper narrationMapper) {
    this.narrationService = narrationService;
    this.imageGenerationService = imageGenerationService;
    this.narrationMapper = narrationMapper;
  }

  @Override
  public String narrate(Fight fight) {
    var domainFight = this.narrationMapper.toFight(fight);
    var narration = this.narrationService.narrate(domainFight);
    Log.debugf("Narration for fight %s = \"%s\"", domainFight, narration);

    return narration;
  }

  @Override
  public FightImage generateImageFromNarration(ImageGenerationRequest request) {
    var domainRequest = this.narrationMapper.toImageGenerationRequest(request);
    var image = this.imageGenerationService.generateImageForNarration(domainRequest);
    Log.debugf("Image (%s) generated from request: %s", image, domainRequest);

    return this.narrationMapper.toApiFightImage(image);
  }
}
