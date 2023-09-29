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

import io.quarkus.sample.superheroes.narration.config.NarrationConfig.OpenAI;
import io.quarkus.sample.superheroes.narration.config.constraints.OpenAIApiOrganizationIdValid.OpenAiOrganizationIdValidator;

@Constraint(validatedBy = OpenAiOrganizationIdValidator.class)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OpenAIApiOrganizationIdValid {
  String message() default "OpenAI configuration is enabled (narration.open-ai.enabled = true) but no organization id (narration.open-ai.organization-id) is specified. Please configure an organization id.";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};

  class OpenAiOrganizationIdValidator implements ConstraintValidator<OpenAIApiOrganizationIdValid, OpenAI> {
    @Override
    public boolean isValid(OpenAI openAI, ConstraintValidatorContext context) {
      return (openAI.enabled() && openAI.organizationId().isEmpty()) ? false : true;
    }
  }
}
