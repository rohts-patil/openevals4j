package com.openevals4j.metrics.exception;

import com.openevals4j.metrics.models.ValidationResult;
import lombok.Getter;

/** Custom exception for EvaluationContext validation failures. */
@Getter
public class EvaluationContextValidationException extends RuntimeException {

  public EvaluationContextValidationException(ValidationResult validationResult) {
    super(validationResult.getErrorMessage());
  }
}
