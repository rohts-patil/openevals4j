package com.openevals4j.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.contextualprecision.ContextualPrecisionMetric;
import com.openevals4j.metrics.contextualrecall.ContextualRecallMetric;
import com.openevals4j.metrics.contextualrelevancy.ContextualRelevancyMetric;
import com.openevals4j.metrics.faithfulness.FaithfulnessMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.responsecompleteness.ResponseCompletenessMetric;
import com.openevals4j.metrics.rubrics.RubricsBasedMetric;
import com.openevals4j.metrics.rubrics.models.RubricCriterion;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LLMBasedMetricFactory {

  private static ContextualRecallMetric createContextualRecallMetric(
      ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    return ContextualRecallMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .build();
  }

  private static FaithfulnessMetric createFaithfulnessMetric(
      ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    return FaithfulnessMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .build();
  }

  private static RubricsBasedMetric createRubricsBasedMetric(
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      List<RubricCriterion> rubricCriteria) {
    return RubricsBasedMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .rubricCriteria(rubricCriteria)
        .build();
  }

  private static ContextualPrecisionMetric createContextualPrecisionMetric(
      ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    return ContextualPrecisionMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .build();
  }

  private static ResponseCompletenessMetric createResponseCompletenessMetric(
      ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    return ResponseCompletenessMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .build();
  }

  private static ContextualRelevancyMetric createContextualRelevancyMetric(
      ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    return ContextualRelevancyMetric.builder()
        .evaluatorLLM(evaluatorLLM)
        .objectMapper(objectMapper)
        .build();
  }

  public static LLMBasedMetric<EvaluationContext, EvaluationResult> createMetric(
      MetricName metricName,
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      List<RubricCriterion> rubricCriteria) {
    return switch (metricName) {
      case CONTEXTUAL_RECALL -> createContextualRecallMetric(evaluatorLLM, objectMapper);
      case FAITHFULNESS -> createFaithfulnessMetric(evaluatorLLM, objectMapper);
      case CONTEXTUAL_PRECISION -> createContextualPrecisionMetric(evaluatorLLM, objectMapper);
      case CONTEXTUAL_RELEVANCY -> createContextualRelevancyMetric(evaluatorLLM, objectMapper);
      case RUBRICS_BASED -> createRubricsBasedMetric(evaluatorLLM, objectMapper, rubricCriteria);
      case RESPONSE_COMPLETENESS -> createResponseCompletenessMetric(evaluatorLLM, objectMapper);
    };
  }
}
