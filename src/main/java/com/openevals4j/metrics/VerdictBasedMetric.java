package com.openevals4j.metrics;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import lombok.Getter;

/**
 * Abstract base class for metrics that generate verdicts (yes/no with reason) for evaluation.
 * Extended by ContextualPrecisionMetric, ContextualRecallMetric, and ContextualRelevancyMetric.
 */
@Getter
public abstract class VerdictBasedMetric
    extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  private final ResponseFormat verdictResponseFormat;
  private final ResponseFormat reasonResponseFormat;

  protected VerdictBasedMetric(
      MetricName metricName, ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super(metricName, evaluatorLLM, objectMapper);
    this.verdictResponseFormat = buildResponseFormatForVerdicts();
    this.reasonResponseFormat = buildResponseFormatForReason();
  }

  protected ResponseFormat buildResponseFormatForVerdicts() {
    return ResponseFormat.builder()
        .type(JSON)
        .jsonSchema(
            JsonSchema.builder()
                .name("Verdicts")
                .rootElement(
                    JsonObjectSchema.builder()
                        .addProperty(
                            "verdicts",
                            JsonArraySchema.builder()
                                .items(
                                    JsonObjectSchema.builder()
                                        .addStringProperty("verdict")
                                        .addStringProperty("reason")
                                        .required("verdict", "reason")
                                        .build())
                                .build())
                        .required("verdicts")
                        .build())
                .build())
        .build();
  }

  protected ResponseFormat buildResponseFormatForReason() {
    return ResponseFormat.builder()
        .type(JSON)
        .jsonSchema(
            JsonSchema.builder()
                .name("Reason")
                .rootElement(
                    JsonObjectSchema.builder()
                        .addStringProperty("reason")
                        .required("reason")
                        .build())
                .build())
        .build();
  }
}
