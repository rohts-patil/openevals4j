package com.openevals4j.metrics.responsecompleteness;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.LLMBasedMetric;
import com.openevals4j.metrics.MetricName;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResponseCompletenessMetric
    extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  @Builder
  public ResponseCompletenessMetric(ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super(MetricName.RESPONSE_COMPLETENESS, evaluatorLLM, objectMapper);
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    validateEvaluationContext(evaluationContext);

    try {
      String prompt =
          String.format(
              ResponseCompletenessPromptConstants.RESPONSE_COMPLETENESS_EVALUATION_PROMPT,
              evaluationContext.getUserInput(),
              evaluationContext.getExpectedResponse(),
              evaluationContext.getActualResponse());

      ChatResponse output = getEvaluatorLLM().chat(buildChatRequest(prompt, getResponseFormat()));

      return getObjectMapper().readValue(output.aiMessage().text(), EvaluationResult.class);

    } catch (JsonProcessingException exception) {
      log.error(
          "Error occurred while evaluating response completeness metric for evaluation context {}",
          evaluationContext,
          exception);
    }

    return getDefaultEvaluationResult();
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("userInput", "expectedResponse", "actualResponse");
  }
}
