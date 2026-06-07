package io.quarkus.sample.superheroes.fight.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import io.quarkus.sample.superheroes.fight.ImageGenerationRequest;
import io.quarkus.sample.superheroes.fight.client.NarrationImageGenerationRequest;

class ImageGenerationRequestMapperTests {
  private static final String DEFAULT_NARRATION = "The hero defeated the villain in an epic battle.";
  private static final String DEFAULT_WINNER_PICTURE_URL = "https://example.com/winner.png";
  private static final String DEFAULT_LOSER_PICTURE_URL = "https://example.com/loser.png";

  ImageGenerationRequestMapper mapper = Mappers.getMapper(ImageGenerationRequestMapper.class);

  @Test
  void mappingWorks() {
    var request = new ImageGenerationRequest(DEFAULT_NARRATION, DEFAULT_WINNER_PICTURE_URL, DEFAULT_LOSER_PICTURE_URL);

    assertThat(this.mapper.toClientRequest(request))
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new NarrationImageGenerationRequest(DEFAULT_NARRATION, DEFAULT_WINNER_PICTURE_URL, DEFAULT_LOSER_PICTURE_URL));
  }

  @Test
  void mappingWorksWithNullUrls() {
    var request = new ImageGenerationRequest(DEFAULT_NARRATION, null, null);

    assertThat(this.mapper.toClientRequest(request))
      .isNotNull()
      .usingRecursiveComparison()
      .isEqualTo(new NarrationImageGenerationRequest(DEFAULT_NARRATION, null, null));
  }
}
