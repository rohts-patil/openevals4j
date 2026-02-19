package com.openevals4j.metrics.contextualrecall;

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

class ContextualRecallMetricTest {

  private ContextualRecallMetric contextualRecallMetric;

  @BeforeEach
  void setUp() {
    ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey("REPLACE_YOUR_API_KEY_HERE")
            .modelName("gemini-1.5-flash")
            .logRequestsAndResponses(true)
            .build();
    contextualRecallMetric =
        ContextualRecallMetric.builder()
            .evaluatorLLM(chatModel)
            .objectMapper(new ObjectMapper())
            .build();
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate() {
    EvaluationResult evaluationResult =
        contextualRecallMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What are the return policies?")
                .expectedResponse("You can return items within 30 days for a full refund.")
                .retrievedContexts(
                    List.of(
                        "All customers are eligible for a 30 day full refund at no extra cost.",
                        "Returns must be in original packaging."))
                .build());

    Assertions.assertEquals(1.0, evaluationResult.getScore());
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluateBatch() {
    List<EvaluationContext> input =
        List.of(
            EvaluationContext.builder()
                .userInput("What are the return policies?")
                .expectedResponse("You can return items within 30 days for a full refund.")
                .retrievedContexts(
                    List.of(
                        "All customers are eligible for a 30 day full refund at no extra cost.",
                        "Returns must be in original packaging."))
                .build(),
            EvaluationContext.builder()
                .userInput("What are the shipping options?")
                .expectedResponse("We offer standard and express shipping.")
                .retrievedContexts(
                    List.of(
                        "We offer standard shipping (3-5 days) and express shipping (1-2 days).",
                        "International shipping is available to select countries."))
                .build());

    List<EvaluationResult> evaluationResults = contextualRecallMetric.evaluateBatch(input);

    Assertions.assertEquals(1.0, evaluationResults.get(0).getScore());
    Assertions.assertEquals(1.0, evaluationResults.get(1).getScore());
  }
}
