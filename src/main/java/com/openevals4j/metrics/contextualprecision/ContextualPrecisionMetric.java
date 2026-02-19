package com.openevals4j.metrics.contextualprecision;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualPrecisionMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  private final String verdictGenerationPrompt;
  private final String reasonGenerationPrompt;

  @Builder
  public ContextualPrecisionMetric(
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      String verdictGenerationPrompt,
      String reasonGenerationPrompt) {
    super(MetricName.CONTEXTUAL_PRECISION, evaluatorLLM, objectMapper);
    this.verdictGenerationPrompt =
        verdictGenerationPrompt != null
            ? verdictGenerationPrompt
            : ContextualPrecisionPromptConstants.VERDICT_GENERATION_PROMPT;
    this.reasonGenerationPrompt =
        reasonGenerationPrompt != null
            ? reasonGenerationPrompt
            : ContextualPrecisionPromptConstants.REASON_GENERATION_PROMPT;
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    validateEvaluationContext(evaluationContext);

    try {
      List<VerdictWithReason> verdicts =
          generateVerdicts(
              evaluationContext.getUserInput(),
              evaluationContext.getExpectedResponse(),
              evaluationContext.getRetrievedContexts());
      double score = calculateScore(verdicts);
      String reason = generateReason(evaluationContext.getUserInput(), score, verdicts);
      return EvaluationResult.builder().score(score).reasoning(reason).build();
    } catch (Exception exception) {
      log.error(
          "Error occurred while evaluating contextual precision metric for evaluation context {}",
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

    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));

    String content = res.content().text().replaceAll(Constants.REGEX, "");

    return extractReason(content);
  }

  private List<VerdictWithReason> generateVerdicts(
      String input, String expectedOutput, List<String> retrievalContext)
      throws JsonProcessingException {

    String prompt = getGenerateVerdictsPrompt(input, expectedOutput, retrievalContext);

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

    List<Integer> nodeVerdicts = new ArrayList<>();
    for (VerdictWithReason verdict : verdicts) {
      nodeVerdicts.add(
          Constants.YES_SMALL_CASE.equalsIgnoreCase(verdict.getVerdict().trim()) ? 1 : 0);
    }

    double sumWeightedPrecisionAtK = 0.0;
    int relevantNodesCount = 0;
    for (int k = 1; k <= nodeVerdicts.size(); k++) {
      if (nodeVerdicts.get(k - 1) == 1) {
        relevantNodesCount++;
        double precisionAtK = (double) relevantNodesCount / k;
        sumWeightedPrecisionAtK += precisionAtK * 1; // isRelevant is always 1 here
      }
    }

    if (relevantNodesCount == 0) {
      return 0;
    }

    return sumWeightedPrecisionAtK / relevantNodesCount;
  }

  private String getGenerateVerdictsPrompt(
      String input, String expectedOutput, List<String> retrievalContext) {
    String documentCountStr =
        " ("
            + retrievalContext.size()
            + " document"
            + (retrievalContext.size() > 1 ? "s" : "")
            + ")";
    return String.format(
        verdictGenerationPrompt, input, expectedOutput, documentCountStr, retrievalContext);
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("userInput", "retrievedContexts");
  }
}
