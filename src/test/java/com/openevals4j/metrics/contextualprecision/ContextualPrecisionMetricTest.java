package com.openevals4j.metrics.contextualprecision;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.context.EvaluationContext;
import com.openevals4j.metrics.context.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ContextualPrecisionMetricTest {

  private ContextualPrecisionMetric contextualPrecisionMetric;

  @BeforeEach
  void setUp() {
    ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey("REPLACE_YOUR_API_KEY_HERE")
            .modelName("gemini-1.5-flash")
            .logRequestsAndResponses(true)
            .build();
    contextualPrecisionMetric =
        ContextualPrecisionMetric.builder()
            .evaluatorLLM(chatModel)
            .objectMapper(new ObjectMapper())
            .build();
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate() {
    EvaluationResult evaluationResult =
        contextualPrecisionMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What if these shoes don't fit?")
                .actualResponse("We offer a 30-day full refund at no extra cost.")
                .expectedResponse("You are eligible for a 30 day full refund at no extra cost")
                .retrievedContexts(
                    List.of(
                        "All customers are eligible for a 30 day full refund at no extra cost."))
                .build());

    Assertions.assertEquals(1.0, evaluationResult.getScore());
  }
}
