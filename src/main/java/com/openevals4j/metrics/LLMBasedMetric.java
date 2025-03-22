package com.openevals4j.metrics;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.exception.EvaluationContextValidationException;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.models.ValidationProfile;
import com.openevals4j.metrics.models.ValidationResult;
import com.openevals4j.metrics.utils.EvaluationContextValidator;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import java.util.List;
import lombok.Data;

@Data
public class LLMBasedMetric<K, V> implements Metric<K, V> {

  private final String metricName;

  private final ChatLanguageModel evaluatorLLM;

  private final ObjectMapper objectMapper;

  private final ResponseFormat responseFormat;

  public LLMBasedMetric(
      String metricName, ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    this.metricName = metricName;
    this.evaluatorLLM = evaluatorLLM;
    this.objectMapper = objectMapper;
    this.responseFormat = buildResponseFormatForScoreAndReasoning();
  }

  @Override
  public V evaluate(K input) {
    return null;
  }

  @Override
  public List<V> evaluateBatch(List<K> inputs) {
    return inputs.stream().map(this::evaluate).toList();
  }

  protected ChatRequest buildChatRequest(String prompt, ResponseFormat responseFormat) {
    return ChatRequest.builder()
        .responseFormat(responseFormat)
        .messages(UserMessage.from(prompt))
        .build();
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

  protected EvaluationResult getDefaultEvaluationResult() {
    return EvaluationResult.builder()
        .score(Double.NaN)
        .reasoning(String.format("Error while evaluating %s metric", getMetricName()))
        .build();
  }

  protected void validateEvaluationContext(EvaluationContext evaluationContext) {
    ValidationResult result =
        EvaluationContextValidator.validate(evaluationContext, getValidationProfile());
    if (!result.isValid()) {
      throw new EvaluationContextValidationException(result);
    }
  }

  protected ValidationProfile getValidationProfile() {
    return null;
  }
}
