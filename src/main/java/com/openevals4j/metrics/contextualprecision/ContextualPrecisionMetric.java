package com.openevals4j.metrics.contextualprecision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.openevals4j.metrics.LLMBasedMetric;
import com.openevals4j.metrics.context.EvaluationContext;
import com.openevals4j.metrics.context.EvaluationResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;

public class ContextualPrecisionMetric extends LLMBasedMetric<EvaluationContext, EvaluationResult> {

  private final ContextualPrecisionTemplate evaluationTemplate = new ContextualPrecisionTemplate();

  @Builder
  public ContextualPrecisionMetric(ChatLanguageModel evaluatorLLM, ObjectMapper objectMapper) {
    super("Contextual Precision", evaluatorLLM, objectMapper);
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    try {
      List<ContextualPrecisionVerdict> verdicts =
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
      String input, double score, List<ContextualPrecisionVerdict> verdicts) {
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
        evaluationTemplate.generateReason(input, String.valueOf(score), retrievalContextsVerdicts);

    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));
    String content = res.content().text().replaceAll("^```json\n|```$", "");
    return extractReason(content);
  }

  private List<ContextualPrecisionVerdict> generateVerdicts(
      String input, String expectedOutput, List<String> retrievalContext)
      throws JsonProcessingException {
    String prompt = evaluationTemplate.generateVerdicts(input, expectedOutput, retrievalContext);
    Response<AiMessage> res = getEvaluatorLLM().generate(new UserMessage(prompt));
    String content = res.content().text().replaceAll("^```json\n|```$", "");
    return getObjectMapper().readValue(content, new TypeReference<>() {});
  }

  private String extractReason(String jsonString) {
    Gson gson = new Gson();
    Type type = new TypeToken<Map<String, String>>() {}.getType();
    Map<String, String> map = gson.fromJson(jsonString, type);
    return map.get("reason");
  }

  private double calculateScore(List<ContextualPrecisionVerdict> verdicts) {
    int numberOfVerdicts = verdicts.size();
    if (numberOfVerdicts == 0) {
      return 0;
    }

    List<Integer> nodeVerdicts = new ArrayList<>();
    for (ContextualPrecisionVerdict verdict : verdicts) {
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

  public static class ContextualPrecisionTemplate {

    public String generateVerdicts(
        String input, String expectedOutput, List<String> retrievalContext) {
      String documentCountStr =
          " ("
              + retrievalContext.size()
              + " document"
              + (retrievalContext.size() > 1 ? "s" : "")
              + ")";
      return String.format(
          """
                    Given the input, expected output, and retrieval context, please generate a list of JSON objects to determine whether each node in the retrieval context was remotely useful in arriving at the expected output.

                    **
                    IMPORTANT: Please make sure to only return in JSON format, with the 'verdicts' key as a list of JSON. These JSON only contain the `verdict` key that outputs only 'yes' or 'no', and a `reason` key to justify the verdict. In your reason, you should aim to quote parts of the context.
                    Example Retrieval Context: ["Einstein won the Nobel Prize for his discovery of the photoelectric effect", "He won the Nobel Prize in 1968.", "There was a cat."]
                    Example Input: "Who won the Nobel Prize in 1968 and for what?"
                    Example Expected Output: "Einstein won the Nobel Prize in 1968 for his discovery of the photoelectric effect."

                    Example:

                        [
                            {
                                "verdict": "yes",
                                "reason": "It clearly addresses the question by stating that 'Einstein won the Nobel Prize for his discovery of the photoelectric effect.'"
                            },
                            {
                                "verdict": "yes",
                                "reason": "The text verifies that the prize was indeed won in 1968."
                            },
                            {
                                "verdict": "no",
                                "reason": "'There was a cat' is not at all relevant to the topic of winning a Nobel Prize."
                            }
                        ]

                    Since you are going to generate a verdict for each context, the number of 'verdicts' SHOULD BE STRICTLY EQUAL to that of the contexts.
                    **

                    Input:
                    %s

                    Expected output:
                    %s

                    Retrieval Context %s:
                     %s
                    """,
          input, expectedOutput, documentCountStr, retrievalContext);
    }

    public String generateReason(String input, String score, List<Map<String, String>> verdicts) {
      return String.format(
          """
                    Given the input, retrieval contexts, and contextual precision score, provide a CONCISE summary for the score. Explain why it is not higher, but also why it is at its current score.
                    The retrieval contexts is a list of JSON with three keys: `verdict`, `reason` (reason for the verdict) and `node`. `verdict` will be either 'yes' or 'no', which represents whether the corresponding 'node' in the retrieval context is relevant to the input.
                    Contextual precision represents if the relevant nodes are ranked higher than irrelevant nodes. Also note that retrieval contexts is given IN THE ORDER OF THEIR RANKINGS.

                    **
                    IMPORTANT: Please make sure to only return in JSON format, with the 'reason' key providing the reason.
                    Example JSON:
                    {
                        "reason": "The score is <contextual_precision_score> because <your_reason>."
                    }

                    DO NOT mention 'verdict' in your reason, but instead phrase it as irrelevant nodes. The term 'verdict' is just here for you to understand the broader scope of things.
                    Also DO NOT mention there are `reason` fields in the retrieval contexts you are presented with, instead just use the information in the `reason` field.
                    In your reason, you MUST USE the `reason`, QUOTES in the 'reason', and the node RANK (starting from 1, eg. first node) to explain why the 'no' verdicts should be ranked lower than the 'yes' verdicts.
                    When addressing nodes, make it explicit that they are nodes in retrieval contexts.
                    If the score is 1, keep it short and say something positive with an upbeat tone (but don't overdo it, otherwise it gets annoying).
                    **

                    Contextual Precision Score:
                    %s

                    Input:
                    %s

                    Retrieval Contexts:
                    %s

                    JSON:
                    """,
          score, input, verdicts);
    }
  }
}
