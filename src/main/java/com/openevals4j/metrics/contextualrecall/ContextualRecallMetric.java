package com.openevals4j.metrics.contextualrecall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.Constants;
import com.openevals4j.metrics.MetricName;
import com.openevals4j.metrics.VerdictBasedMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.models.VerdictWithReason;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualRecallMetric extends VerdictBasedMetric {

  private final String verdictGenerationPrompt;
  private final String reasonGenerationPrompt;

  @Builder
  public ContextualRecallMetric(
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      String verdictGenerationPrompt,
      String reasonGenerationPrompt) {
    super(MetricName.CONTEXTUAL_RECALL, evaluatorLLM, objectMapper);
    this.verdictGenerationPrompt =
        verdictGenerationPrompt != null
            ? verdictGenerationPrompt
            : ContextualRecallPromptConstants.VERDICT_GENERATION_PROMPT;
    this.reasonGenerationPrompt =
        reasonGenerationPrompt != null
            ? reasonGenerationPrompt
            : ContextualRecallPromptConstants.REASON_GENERATION_PROMPT;
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    validateEvaluationContext(evaluationContext);
    try {
      List<VerdictWithReason> verdicts =
          generateVerdicts(
              evaluationContext.getExpectedResponse(), evaluationContext.getRetrievedContexts());
      double score = calculateScore(verdicts);
      String reason = generateReason(evaluationContext.getExpectedResponse(), score, verdicts);
      return EvaluationResult.builder().score(score).reasoning(reason).build();
    } catch (Exception exception) {
      log.error(
          "Error occurred while evaluating contextual recall metric for evaluation context {}",
          evaluationContext,
          exception);
      return getDefaultEvaluationResult();
    }
  }

  private String generateReason(
      String expectedResponse, double score, List<VerdictWithReason> verdicts)
      throws JsonProcessingException {
    List<String> supportiveReasons = new ArrayList<>();
    List<String> unSupportiveReasons = new ArrayList<>();

    for (VerdictWithReason verdict : verdicts) {
      if (Constants.YES_SMALL_CASE.equalsIgnoreCase(verdict.getVerdict().trim())) {
        supportiveReasons.add(verdict.getReason());
      } else {
        unSupportiveReasons.add(verdict.getReason());
      }
    }

    String prompt =
        String.format(
            reasonGenerationPrompt,
            score,
            expectedResponse,
            supportiveReasons,
            unSupportiveReasons);

    ChatResponse response =
        getEvaluatorLLM().chat(buildChatRequest(prompt, getReasonResponseFormat()));

    String content = response.aiMessage().text();

    return extractReason(content);
  }

  private List<VerdictWithReason> generateVerdicts(
      String expectedOutput, List<String> retrievalContext) throws JsonProcessingException {

    String prompt = String.format(verdictGenerationPrompt, expectedOutput, retrievalContext);

    ChatResponse response =
        getEvaluatorLLM().chat(buildChatRequest(prompt, getVerdictResponseFormat()));

    String content = response.aiMessage().text();

    Map<String, List<VerdictWithReason>> wrapper =
        getObjectMapper().readValue(content, new TypeReference<>() {});
    return wrapper.get("verdicts");
  }

  private String extractReason(String jsonString) throws JsonProcessingException {
    return (String) getObjectMapper().readValue(jsonString, Map.class).get("reason");
  }

  private double calculateScore(List<VerdictWithReason> verdicts) {
    int numberOfVerdicts = verdicts.size();

    if (numberOfVerdicts == 0) {
      return 0;
    }

    int justifiedVerdicts = 0;

    for (VerdictWithReason verdict : verdicts) {
      if (Constants.YES_SMALL_CASE.equalsIgnoreCase(verdict.getVerdict().trim())) {
        justifiedVerdicts++;
      }
    }

    return (double) justifiedVerdicts / numberOfVerdicts;
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("expectedResponse", "retrievedContexts");
  }
}
