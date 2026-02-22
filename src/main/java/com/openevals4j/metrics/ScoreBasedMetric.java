package com.openevals4j.metrics;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import lombok.Getter;

/**
 * Abstract base class for metrics that produce a score and reasoning as their LLM response format.
 * Extended by FaithfulnessMetric, ResponseCompletenessMetric, and RubricsBasedMetric.
 */
@Getter
public abstract class ScoreBasedMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  private final ResponseFormat responseFormat;

  protected ScoreBasedMetric(
      MetricName metricName, ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super(metricName, evaluatorLLM, objectMapper);
    this.responseFormat = buildResponseFormatForScoreAndReasoning();
  }

  protected ResponseFormat buildResponseFormatForScoreAndReasoning() {
    return ResponseFormat.builder()
        .type(JSON)
        .jsonSchema(
            JsonSchema.builder()
                .name("EvaluationResult")
                .rootElement(
                    JsonObjectSchema.builder()
                        .addStringProperty("reasoning")
                        .addNumberProperty("score")
                        .required("reasoning", "score")
                        .build())
                .build())
        .build();
  }
}
