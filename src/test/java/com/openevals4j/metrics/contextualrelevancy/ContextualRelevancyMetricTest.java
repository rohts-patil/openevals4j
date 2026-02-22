package com.openevals4j.metrics.contextualrelevancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ContextualRelevancyMetricTest {

  private ContextualRelevancyMetric contextualRelevancyMetric;

  @BeforeEach
  void setUp() {
    ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey("REPLACE_YOUR_API_KEY_HERE")
            .modelName("gemini-2.5-flash")
            .logRequestsAndResponses(true)
            .build();
    contextualRelevancyMetric =
        ContextualRelevancyMetric.builder()
            .evaluatorLLM(chatModel)
            .objectMapper(new ObjectMapper())
            .build();
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate() {
    EvaluationResult evaluationResult =
        contextualRelevancyMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What is the capital of France?")
                .retrievedContexts(
                    List.of(
                        "The capital of France is Paris.",
                        "Paris is the largest city in France.",
                        "France is a country in Western Europe."))
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 0.0 && evaluationResult.getScore() <= 1.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
  }
}
