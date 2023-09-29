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

import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.sample.superheroes.narration.config.constraints.SingleOpenAIImplementation.SingleOpenAIImplementationValidator;

@Constraint(validatedBy = SingleOpenAIImplementationValidator.class)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SingleOpenAIImplementation {
  String message() default "Both OpenAI (narration.open-ai.enabled) and Azure OpenAI (narration.azure-open-ai.enabled) configurations are enabled. You can only have one.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  class SingleOpenAIImplementationValidator implements ConstraintValidator<SingleOpenAIImplementation, NarrationConfig> {
    @Override
    public boolean isValid(NarrationConfig narrationConfig, ConstraintValidatorContext context) {
      return !(narrationConfig.azureOpenAi().enabled() &&
        narrationConfig.openAi().enabled());
    }
  }
}
