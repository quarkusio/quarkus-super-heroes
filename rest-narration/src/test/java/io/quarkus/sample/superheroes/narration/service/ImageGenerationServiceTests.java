package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.ModelName;

@QuarkusTest
class ImageGenerationServiceTests {
  private static final String DEFAULT_IMAGE_URL = "https://somewhere.com/someImage.png";
  private static final String DEFAULT_IMAGE_NARRATION = "Alternate image narration";
  private static final String NARRATION = "Lorem ipsum dolor sit amet";

  @InjectMock
  @ModelName("dalle3")
  ImageModel imageModel;

  @Inject
  ImageGenerationService imageGenerationService;

  @Test
  void generateImageForNarration() {
    var image = Image.builder()
      .url(DEFAULT_IMAGE_URL)
      .revisedPrompt(DEFAULT_IMAGE_NARRATION)
      .build();

    when(this.imageModel.generate(NARRATION))
      .thenReturn(new Response<>(image));

    assertThat(this.imageGenerationService.generateImageForNarration(NARRATION))
      .isNotNull()
      .extracting(
        FightImage::imageNarration,
        FightImage::imageUrl
      )
      .containsExactly(
        image.revisedPrompt(),
        image.url().toString()
      );

    verify(this.imageModel).generate(NARRATION);
    verifyNoMoreInteractions(this.imageModel);
  }
}
