package io.quarkus.sample.superheroes.narration.jackson;

import io.quarkus.jackson.JacksonMixin;

import io.quarkus.sample.superheroes.narration.jackson.GenerateImagesRequestMixin.ModerationPropertyWriter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;
import dev.langchain4j.model.openai.internal.image.GenerateImagesRequest;

@JacksonMixin(GenerateImagesRequest.class)
@JsonAppend(props = @JsonAppend.Prop(value = ModerationPropertyWriter.class, name = "moderation", type = String.class))
@JsonIgnoreProperties({ "style", "response_format" })
public interface GenerateImagesRequestMixin {
  class ModerationPropertyWriter extends VirtualBeanPropertyWriter {
    public ModerationPropertyWriter() {
    }

    public ModerationPropertyWriter(BeanPropertyDefinition propDef, Annotations contextAnnotations, JavaType declaredType) {
      super(propDef, contextAnnotations, declaredType);
    }

    @Override
    protected Object value(Object bean, JsonGenerator gen, SerializerProvider prov) {
      return "low";
    }

    @Override
    public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config, AnnotatedClass declaringClass, BeanPropertyDefinition propDef, JavaType type) {
      return new ModerationPropertyWriter(propDef, declaringClass.getAnnotations(), type);
    }
  }
}
