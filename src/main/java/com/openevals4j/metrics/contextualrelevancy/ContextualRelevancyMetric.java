package com.openevals4j.metrics.contextualrelevancy;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualRelevancyMetric extends VerdictBasedMetric {

  private final String verdictGenerationPrompt;
  private final String reasonGenerationPrompt;

  @Builder
  public ContextualRelevancyMetric(
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      String verdictGenerationPrompt,
      String reasonGenerationPrompt) {
    super(MetricName.CONTEXTUAL_RELEVANCY, evaluatorLLM, objectMapper);
    this.verdictGenerationPrompt =
        verdictGenerationPrompt != null
            ? verdictGenerationPrompt
            : ContextualRelevancyPromptConstants.VERDICT_GENERATION_PROMPT;
    this.reasonGenerationPrompt =
        reasonGenerationPrompt != null
            ? reasonGenerationPrompt
            : ContextualRelevancyPromptConstants.REASON_GENERATION_PROMPT;
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    validateEvaluationContext(evaluationContext);

    try {
      List<VerdictWithReason> verdicts =
          generateVerdicts(
              evaluationContext.getUserInput(), evaluationContext.getRetrievedContexts());
      double score = calculateScore(verdicts);
      String reason = generateReason(evaluationContext.getUserInput(), score, verdicts);
      return EvaluationResult.builder().score(score).reasoning(reason).build();
    } catch (Exception exception) {
      log.error(
          "Error occurred while evaluating contextual relevancy metric for evaluation context {}",
          evaluationContext,
          exception);
      return getDefaultEvaluationResult();
    }
  }

  private String generateReason(String input, double score, List<VerdictWithReason> verdicts)
      throws JsonProcessingException {
    List<Map<String, String>> retrievalContextsVerdicts =
        verdicts.stream()
            .map(
                verdict -> {
                  Map<String, String> map = new HashMap<>();
                  map.put("verdict", verdict.getVerdict());
                  map.put("reason", verdict.getReason());
                  return map;
                })
            .toList();

    String prompt = String.format(reasonGenerationPrompt, score, input, retrievalContextsVerdicts);

    ChatResponse response =
        getEvaluatorLLM().chat(buildChatRequest(prompt, getReasonResponseFormat()));

    String content = response.aiMessage().text();

    return extractReason(content);
  }

  private List<VerdictWithReason> generateVerdicts(String input, List<String> retrievalContext)
      throws JsonProcessingException {

    String prompt = getGenerateVerdictsPrompt(input, retrievalContext);

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
    if (verdicts.isEmpty()) {
      return 0.0;
    }

    long relevantCount =
        verdicts.stream()
            .mapToLong(
                verdict ->
                    Constants.YES_SMALL_CASE.equalsIgnoreCase(verdict.getVerdict().trim()) ? 1 : 0)
            .sum();

    return (double) relevantCount / verdicts.size();
  }

  private String getGenerateVerdictsPrompt(String input, List<String> retrievalContext) {
    String documentCountStr =
        " ("
            + retrievalContext.size()
            + " document"
            + (retrievalContext.size() > 1 ? "s" : "")
            + ")";
    return String.format(verdictGenerationPrompt, input, documentCountStr, retrievalContext);
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("userInput", "retrievedContexts");
  }
}
