package io.quarkus.sample.superheroes.narration.jackson;

import io.quarkus.jackson.JacksonMixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.langchain4j.model.openai.internal.image.GenerateImagesRequest;

@JacksonMixin(GenerateImagesRequest.class)
@JsonIgnoreProperties("style")
public interface GenerateImagesRequestMixin {
}
