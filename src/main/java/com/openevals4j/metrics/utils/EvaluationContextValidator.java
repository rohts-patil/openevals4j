package com.openevals4j.metrics.utils;

import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.ValidationProfile;
import com.openevals4j.metrics.models.ValidationResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Utility class for validating EvaluationContext objects based on different use case requirements.
 */
public class EvaluationContextValidator {

  /**
   * Validates an EvaluationContext based on specified required fields.
   *
   * @param context The EvaluationContext to validate
   * @param requiredFields A list of field names that are required to be non-empty/non-null
   * @return A ValidationResult containing validation status and any error messages
   */
  public static ValidationResult validate(EvaluationContext context, List<String> requiredFields) {
    if (Objects.isNull(context)) {
      return ValidationResult.builder()
          .isValid(false)
          .errorMessage("EvaluationContext cannot be null")
          .build();
    }

    List<String> missingFields = new ArrayList<>();
    Map<String, Function<EvaluationContext, Boolean>> fieldValidators = createFieldValidators();

    for (String fieldName : requiredFields) {
      Function<EvaluationContext, Boolean> validator = fieldValidators.get(fieldName);

      if (Objects.isNull(validator)) {
        return ValidationResult.builder()
            .isValid(false)
            .errorMessage("Unknown field name: " + fieldName)
            .build();
      }

      if (!validator.apply(context)) {
        missingFields.add(fieldName);
      }
    }

    if (!missingFields.isEmpty()) {
      return ValidationResult.builder()
          .isValid(false)
          .errorMessage("Missing required fields: " + String.join(", ", missingFields))
          .missingFields(missingFields)
          .build();
    }

    return ValidationResult.builder().isValid(true).build();
  }

  /** Creates a map of field name to validator functions */
  private static Map<String, Function<EvaluationContext, Boolean>> createFieldValidators() {
    Map<String, Function<EvaluationContext, Boolean>> validators = new HashMap<>();

    validators.put("userInput", context -> (isStringNotEmpty(context.getUserInput())));

    validators.put("actualResponse", context -> (isStringNotEmpty(context.getActualResponse())));

    validators.put(
        "expectedResponse", context -> (isStringNotEmpty(context.getExpectedResponse())));

    validators.put(
        "retrievedContexts", context -> (isListNotEmpty(context.getRetrievedContexts())));

    validators.put(
        "referenceContexts", context -> (isListNotEmpty(context.getReferenceContexts())));

    validators.put("metadata", context -> !Objects.isNull(context.getMetadata()));

    return validators;
  }

  private static boolean isStringNotEmpty(String string) {
    return !Objects.isNull(string) && !string.isEmpty();
  }

  private static boolean isListNotEmpty(List<String> list) {
    return !Objects.isNull(list) && !list.isEmpty();
  }

  /**
   * Validates an EvaluationContext based on a predefined validation profile.
   *
   * @param context The EvaluationContext to validate
   * @param profile The predefined ValidationProfile to use
   * @return A ValidationResult containing validation status and any error messages
   */
  public static ValidationResult validate(EvaluationContext context, ValidationProfile profile) {
    return validate(context, profile.getRequiredFields());
  }
}
