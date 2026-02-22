package com.openevals4j.metrics.rubrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import com.openevals4j.metrics.rubrics.models.RubricCriterion;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RubricsBasedMetricTest {

  private RubricsBasedMetric rubricsBasedMetric;

  private List<RubricCriterion> createSampleRubricCriteria() {
    Map<Integer, String> accuracyGuidelines = new HashMap<>();
    accuracyGuidelines.put(1, "Contains multiple factual errors");
    accuracyGuidelines.put(2, "Contains some factual errors");
    accuracyGuidelines.put(3, "Mostly accurate with minor errors");
    accuracyGuidelines.put(4, "Accurate with very minor issues");
    accuracyGuidelines.put(5, "Completely accurate with no errors");

    Map<Integer, String> completenessGuidelines = new HashMap<>();
    completenessGuidelines.put(1, "Addresses almost none of the question");
    completenessGuidelines.put(2, "Addresses a small portion of the question");
    completenessGuidelines.put(3, "Addresses about half of the question");
    completenessGuidelines.put(4, "Addresses most aspects of the question");
    completenessGuidelines.put(5, "Comprehensively addresses all aspects of the question");

    Map<Integer, String> clarityGuidelines = new HashMap<>();
    clarityGuidelines.put(1, "Very difficult to understand");
    clarityGuidelines.put(2, "Somewhat difficult to understand");
    clarityGuidelines.put(3, "Moderately clear with some confusing parts");
    clarityGuidelines.put(4, "Mostly clear and well-organized");
    clarityGuidelines.put(5, "Exceptionally clear, well-organized, and easy to understand");

    return List.of(
        RubricCriterion.builder()
            .name("Accuracy")
            .description("The factual correctness of the information provided")
            .weight(0.4)
            .scoringGuidelines(accuracyGuidelines)
            .build(),
        RubricCriterion.builder()
            .name("Completeness")
            .description("How thoroughly the response addresses all aspects of the question")
            .weight(0.3)
            .scoringGuidelines(completenessGuidelines)
            .build(),
        RubricCriterion.builder()
            .name("Clarity")
            .description("How clear and well-organized the response is")
            .weight(0.3)
            .scoringGuidelines(clarityGuidelines)
            .build());
  }

  @BeforeEach
  void setUp() {
    ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey("REPLACE_YOUR_API_KEY_HERE")
            .modelName("gemini-2.5-flash")
            .logRequestsAndResponses(true)
            .build();
    rubricsBasedMetric =
        RubricsBasedMetric.builder()
            .evaluatorLLM(chatModel)
            .objectMapper(new ObjectMapper())
            .rubricCriteria(createSampleRubricCriteria())
            .build();
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate() {
    EvaluationResult evaluationResult =
        rubricsBasedMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What are the main causes of climate change?")
                .actualResponse(
                    "Climate change is primarily caused by the burning of fossil fuels like coal, oil, and natural gas, "
                        + "which releases greenhouse gases into the atmosphere. These gases trap heat from the sun, "
                        + "causing the Earth's temperature to rise. Deforestation also contributes to climate change "
                        + "by reducing the number of trees that can absorb carbon dioxide.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 0.0 && evaluationResult.getScore() <= 1.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    Assertions.assertNotNull(evaluationResult.getDebugData());
    Assertions.assertTrue(evaluationResult.getDebugData().containsKey("criteriaScores"));
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluateBatch() {
    List<EvaluationContext> inputs =
        List.of(
            EvaluationContext.builder()
                .userInput("What is the capital of France?")
                .actualResponse("The capital of France is Paris.")
                .build(),
            EvaluationContext.builder()
                .userInput("What are the benefits of exercise?")
                .actualResponse("Exercise improves cardiovascular health and mood.")
                .build());

    List<EvaluationResult> results = rubricsBasedMetric.evaluateBatch(inputs);

    Assertions.assertEquals(2, results.size());
    for (EvaluationResult result : results) {
      Assertions.assertTrue(result.getScore() >= 0.0 && result.getScore() <= 1.0);
      Assertions.assertNotNull(result.getReasoning());
    }
  }
}
