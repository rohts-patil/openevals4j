package com.openevals4j.metrics.contextualprecision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.Constants;
import com.openevals4j.metrics.LLMBasedMetric;
import com.openevals4j.metrics.context.EvaluationContext;
import com.openevals4j.metrics.context.EvaluationResult;
import com.openevals4j.metrics.context.VerdictWithReason;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

public class ContextualPrecisionMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  @Builder
  public ContextualPrecisionMetric(ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super("Contextual Precision", evaluatorLLM, objectMapper);
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    try {
      List<VerdictWithReason> verdicts =
          generateVerdicts(
              evaluationContext.getUserInput(),
              evaluationContext.getExpectedResponse(),
              evaluationContext.getRetrievedContexts());
      double score = calculateScore(verdicts);
      String reason = generateReason(evaluationContext.getUserInput(), score, verdicts);
      return EvaluationResult.builder().score(score).reasoning(reason).build();
    } catch (Exception e) {
      return getDefaultEvaluationResult();
    }
  }

  private String generateReason(
      String input, double score, List<VerdictWithReason> verdicts)
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
            ContextualPrecisionPromptConstants.REASON_GENERATION_PROMPT,
            score,
            input,
            retrievalContextsVerdicts);

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
      nodeVerdicts.add(verdict.getVerdict().trim().equalsIgnoreCase("yes") ? 1 : 0);
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
        ContextualPrecisionPromptConstants.VERDICT_GENERATION_PROMPT,
        input,
        expectedOutput,
        documentCountStr,
        retrievalContext);
  }
}
