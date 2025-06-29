package com.openevals4j.metrics.contextualrelevancy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.Constants;
import com.openevals4j.metrics.LLMBasedMetric;
import com.openevals4j.metrics.MetricName;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.models.VerdictWithReason;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualRelevancyMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  @Builder
  public ContextualRelevancyMetric(ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super(MetricName.CONTEXTUAL_RELEVANCY, evaluatorLLM, objectMapper);
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

    String prompt =
        String.format(
            ContextualRelevancyPromptConstants.REASON_GENERATION_PROMPT,
            score,
            input,
            retrievalContextsVerdicts);

    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));

    String content = res.content().text().replaceAll(Constants.REGEX, "");

    return extractReason(content);
  }

  private List<VerdictWithReason> generateVerdicts(String input, List<String> retrievalContext)
      throws JsonProcessingException {

    String prompt = getGenerateVerdictsPrompt(input, retrievalContext);

    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));

    String content = res.content().text().replaceAll(Constants.REGEX, "");

    return getObjectMapper().readValue(content, new TypeReference<>() {});
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
    return String.format(
        ContextualRelevancyPromptConstants.VERDICT_GENERATION_PROMPT,
        input,
        documentCountStr,
        retrievalContext);
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("userInput", "retrievedContexts");
  }
}
