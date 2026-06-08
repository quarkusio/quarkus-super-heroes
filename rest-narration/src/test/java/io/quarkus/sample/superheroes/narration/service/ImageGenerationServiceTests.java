package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import io.quarkus.sample.superheroes.narration.FightImage;
import io.quarkus.sample.superheroes.narration.ImageGenerationRequest;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;

@QuarkusTest
class ImageGenerationServiceTests {
  private static final String NARRATION = "Lorem ipsum dolor sit amet";
  private static final String PROMPT = ImageGenerationService.SYSTEM_MESSAGE + "\nHere is the fight narration: \"" + NARRATION + "\"";

  @InjectMock
  ImageModel imageModel;

  @Inject
  ImageGenerationService imageGenerationService;

  @Test
  void generateImageForNarration() {
    var image = Image.builder()
      .revisedPrompt("Alternate image narration")
      .base64Data("base64Data")
      .mimeType("image/png")
      .build();

    when(this.imageModel.generate(startsWith(PROMPT)))
      .thenReturn(Response.from(image));

    assertThat(this.imageGenerationService.generateImageForNarration(new ImageGenerationRequest(NARRATION, "http://somewhere.com", "http://somewhereelse.com")))
      .isNotNull()
      .extracting(FightImage::imageUrl)
      .isEqualTo("data:image/png;base64,base64Data");

    verify(this.imageModel).generate(startsWith(PROMPT));
    verifyNoMoreInteractions(this.imageModel);
  }
}
