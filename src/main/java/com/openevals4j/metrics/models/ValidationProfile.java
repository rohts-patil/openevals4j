package com.openevals4j.metrics.models;

import java.util.List;
import lombok.Getter;

/** Enum representing predefined validation profiles for common use cases */
@Getter
public enum ValidationProfile {
  EXPECTATION_COMPARISON(
      "expectationComparisonProfile", List.of("actualResponse", "expectedResponse")),
  CONTEXTUAL_PRECISION("contextualPrecisionProfile", List.of("userInput", "retrievedContexts")),
  CONTEXTUAL_RECALL("contextualRecallProfile", List.of("expectedResponse", "retrievedContexts")),
  FAITHFULNESS("faithfulnessProfile", List.of("userInput", "actualResponse", "retrievedContexts")),
  FULL_EVALUATION(
      "fullEvaluationProfile",
      List.of(
          "userInput",
          "actualResponse",
          "expectedResponse",
          "retrievedContexts",
          "referenceContexts")),
  RUBRICS_BASED(
      "rubricsBasedProfile",
      List.of("userInput", "actualResponse"));

  private final String profileName;
  private final List<String> requiredFields;

  ValidationProfile(String profileName, List<String> requiredFields) {
    this.profileName = profileName;
    this.requiredFields = requiredFields;
  }
}
