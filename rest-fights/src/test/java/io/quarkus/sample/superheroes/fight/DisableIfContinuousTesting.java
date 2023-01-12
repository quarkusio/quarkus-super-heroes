package io.quarkus.sample.superheroes.fight;

import io.quarkus.sample.superheroes.fight.DisableIfContinuousTesting.ContinuousTestingCondition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(ContinuousTestingCondition.class)
public @interface DisableIfContinuousTesting {

    class ContinuousTestingCondition implements ExecutionCondition {
      @Override
      public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final Class clazz = context.getRequiredTestClass();
        final var annotation = clazz.getDeclaredAnnotation(DisableIfContinuousTesting.class);
        if (annotation == null) {
          throw new ExtensionConfigurationException("Could not find @" + DisableIfContinuousTesting.class + " annotation on the class " + clazz);
        }

        boolean isContinuousTesting = false;

        // We can't use LaunchMode.NORMAL.equals(LaunchMode.current()), because it's always TEST for tests
        // Instead, check our launch stack trace
        final var st = new Exception().getStackTrace();
        for (final var s : st) {
          // This is fragile, but hopefully temporary
          if (s.getClassName().contains("io.quarkus.deployment.dev.testing")) {
            isContinuousTesting = true;
            break;
          }
        }
        return isContinuousTesting ? ConditionEvaluationResult.disabled("launched in dev or test mode"): ConditionEvaluationResult.enabled("launched in normal mode");
      }
    }
  }

