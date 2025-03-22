package com.openevals4j.metrics.contextualrecall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.Constants;
import com.openevals4j.metrics.LLMBasedMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.models.ValidationProfile;
import com.openevals4j.metrics.models.VerdictWithReason;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;

public class ContextualRecallMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  @Builder
  public ContextualRecallMetric(ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super("Contextual Recall", evaluatorLLM, objectMapper);
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
    } catch (Exception e) {
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
            ContextualRecallPromptConstants.REASON_GENERATION_PROMPT,
            score,
            expectedResponse,
            supportiveReasons,
            unSupportiveReasons);

    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));

    String content = res.content().text().replaceAll(Constants.REGEX, "");

    return extractReason(content);
  }

  private List<VerdictWithReason> generateVerdicts(
      String expectedOutput, List<String> retrievalContext) throws JsonProcessingException {

    String prompt =
        String.format(
            ContextualRecallPromptConstants.VERDICT_GENERATION_PROMPT,
            expectedOutput,
            retrievalContext);
    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));

    String content = res.content().text().replaceAll(Constants.REGEX, "");

    return getObjectMapper().readValue(content, new TypeReference<>() {});
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
  protected ValidationProfile getValidationProfile() {
    return ValidationProfile.CONTEXTUAL_RECALL;
  }
}
