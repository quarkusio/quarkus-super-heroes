package io.quarkus.sample.superheroes.narration.config.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import io.quarkus.sample.superheroes.narration.config.NarrationConfig.AzureOpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.AzureOpenAIEndpointValid.AzureOpenAiEndpointValidator;

@Constraint(validatedBy = AzureOpenAiEndpointValidator.class)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AzureOpenAIEndpointValid {
  String message() default "Azure OpenAI configuration is enabled (narration.azure-open-ai.enabled = true) but no endpoint (narration.azure-open-ai.endpoint) is specified. Please configure an endpoint.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  class AzureOpenAiEndpointValidator implements ConstraintValidator<AzureOpenAIEndpointValid, AzureOpenAI> {
    @Override
    public boolean isValid(AzureOpenAI azureOpenAI, ConstraintValidatorContext context) {
      return (azureOpenAI.enabled() && azureOpenAI.endpoint().isEmpty()) ? false : true;
    }
  }
}
