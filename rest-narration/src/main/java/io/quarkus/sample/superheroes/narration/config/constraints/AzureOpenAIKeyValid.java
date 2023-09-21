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
import io.quarkus.sample.superheroes.narration.config.constraints.AzureOpenAIKeyValid.AzureOpenAiKeyValidator;

@Constraint(validatedBy = AzureOpenAiKeyValidator.class)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AzureOpenAIKeyValid {
  String message() default "Azure OpenAI configuration is enabled (narration.azure-open-ai.enabled = true) but no key (narration.azure-open-ai.key) is specified. Please configure a key.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  class AzureOpenAiKeyValidator implements ConstraintValidator<AzureOpenAIKeyValid, AzureOpenAI> {
    @Override
    public boolean isValid(AzureOpenAI azureOpenAI, ConstraintValidatorContext context) {
      return (azureOpenAI.enabled() && azureOpenAI.key().isEmpty()) ? false : true;
    }
  }
}
