package com.openevals4j.metrics.rubrics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.MetricName;
import com.openevals4j.metrics.ScoreBasedMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.rubrics.models.CriterionScore;
import com.openevals4j.metrics.rubrics.models.RubricCriterion;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class RubricsBasedMetric extends ScoreBasedMetric {

  private final List<RubricCriterion> rubricCriteria;
  private final String evaluationPrompt;

  @Builder
  public RubricsBasedMetric(
      ChatLanguageModel evaluatorLLM,
      ObjectMapper objectMapper,
      List<RubricCriterion> rubricCriteria,
      String evaluationPrompt) {
    super(MetricName.RUBRICS_BASED, evaluatorLLM, objectMapper);

    if (rubricCriteria == null || rubricCriteria.isEmpty()) {
      throw new IllegalArgumentException("Rubric criteria must be provided");
    }

    this.rubricCriteria = rubricCriteria;
    this.evaluationPrompt =
        evaluationPrompt != null
            ? evaluationPrompt
            : RubricsPromptConstants.RUBRICS_EVALUATION_PROMPT;
  }

  @Override
  public EvaluationResult evaluate(EvaluationContext evaluationContext) {
    validateEvaluationContext(evaluationContext);

    try {
      String formattedRubric = formatRubricCriteria(rubricCriteria);
      String prompt =
          String.format(
              evaluationPrompt,
              formattedRubric,
              evaluationContext.getUserInput(),
              evaluationContext.getActualResponse());

      ChatResponse output = getEvaluatorLLM().chat(buildChatRequest(prompt, getResponseFormat()));

      // Parse the response
      Map<String, Object> responseMap =
          getObjectMapper().readValue(output.aiMessage().text(), new TypeReference<>() {});

      // Extract criteria scores
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> criteriaScoresMap =
          (List<Map<String, Object>>) responseMap.get("criteriaScores");

      if (criteriaScoresMap == null || criteriaScoresMap.isEmpty()) {
        throw new IllegalStateException("No criteria scores returned from LLM");
      }

      // Convert to CriterionScore objects
      List<CriterionScore> criteriaScores =
          criteriaScoresMap.stream()
              .map(
                  map ->
                      CriterionScore.builder()
                          .criterion((String) map.get("criterion"))
                          .score(((Number) map.get("score")).intValue())
                          .justification((String) map.get("justification"))
                          .build())
              .toList();

      // Calculate weighted score
      double weightedScore = calculateWeightedScore(criteriaScores);

      // Generate reasoning based on criteria scores
      String reasoning = generateReasoning(criteriaScores, weightedScore);

      // Add criteria scores to debug data
      Map<String, Object> debugData = new HashMap<>();
      debugData.put("criteriaScores", criteriaScores);

      return EvaluationResult.builder()
          .score(weightedScore)
          .reasoning(reasoning)
          .debugData(debugData)
          .build();

    } catch (Exception exception) {
      log.error(
          "Error occurred while evaluating rubrics-based metric for criteria {} and evaluation context {}",
          rubricCriteria,
          evaluationContext,
          exception);
      return getDefaultEvaluationResult();
    }
  }

  /** Calculates the weighted score based on individual criterion scores and their weights */
  private double calculateWeightedScore(List<CriterionScore> criteriaScores) {
    double totalWeightedScore = 0.0;
    double totalWeight = 0.0;

    // Map criterion names to their weights
    Map<String, Double> criterionWeights =
        rubricCriteria.stream()
            .collect(
                HashMap::new,
                (map, criterion) -> map.put(criterion.getName(), criterion.getWeight()),
                HashMap::putAll);

    for (CriterionScore criterionScore : criteriaScores) {
      String criterionName = criterionScore.getCriterion();
      Double weight = criterionWeights.get(criterionName);

      if (weight != null) {
        totalWeightedScore += criterionScore.getScore() * weight;
        totalWeight += weight;
      }
    }

    // Normalize to 0-1 scale (scores are 1-5)
    return totalWeight > 0 ? (totalWeightedScore / totalWeight) / 5.0 : 0.0;
  }

  /** Generates a reasoning string based on the criteria scores and final weighted score */
  private String generateReasoning(List<CriterionScore> criteriaScores, double weightedScore) {
    StringBuilder reasoning = new StringBuilder();
    reasoning.append(String.format("Overall score: %.2f/1.0\n\n", weightedScore));
    reasoning.append("Individual criteria scores:\n");

    for (CriterionScore criterionScore : criteriaScores) {
      reasoning.append(
          String.format(
              "- %s: %d/5 - %s\n",
              criterionScore.getCriterion(),
              criterionScore.getScore(),
              criterionScore.getJustification()));
    }

    return reasoning.toString();
  }

  /** Formats the rubric criteria into a string representation for the prompt */
  private String formatRubricCriteria(List<RubricCriterion> criteria) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < criteria.size(); i++) {
      RubricCriterion criterion = criteria.get(i);

      builder
          .append("### Criterion ")
          .append(i + 1)
          .append(": ")
          .append(criterion.getName())
          .append("\n");
      builder.append("**Description**: ").append(criterion.getDescription()).append("\n");
      builder.append("**Scoring Guidelines**:\n");

      for (Map.Entry<Integer, String> guideline : criterion.getScoringGuidelines().entrySet()) {
        builder
            .append("- **")
            .append(guideline.getKey())
            .append("**: ")
            .append(guideline.getValue())
            .append("\n");
      }

      builder.append("\n");
    }

    return builder.toString();
  }

  @Override
  protected ResponseFormat buildResponseFormatForScoreAndReasoning() {
    return ResponseFormat.builder()
        .type(dev.langchain4j.model.chat.request.ResponseFormatType.JSON)
        .jsonSchema(
            dev.langchain4j.model.chat.request.json.JsonSchema.builder()
                .name("RubricsEvaluation")
                .rootElement(
                    dev.langchain4j.model.chat.request.json.JsonObjectSchema.builder()
                        .addProperty(
                            "criteriaScores",
                            dev.langchain4j.model.chat.request.json.JsonArraySchema.builder()
                                .items(
                                    dev.langchain4j.model.chat.request.json.JsonObjectSchema
                                        .builder()
                                        .addStringProperty("criterion")
                                        .addNumberProperty("score")
                                        .addStringProperty("justification")
                                        .required("criterion", "score", "justification")
                                        .build())
                                .build())
                        .required("criteriaScores")
                        .build())
                .build())
        .build();
  }

  @Override
  protected List<String> getRequiredFieldsForValidation() {
    return List.of("userInput", "actualResponse");
  }
}
