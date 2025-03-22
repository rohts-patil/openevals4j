package com.openevals4j.metrics.models;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidationResult {
  private boolean isValid;
  private String errorMessage;
  private List<String> missingFields;
}
