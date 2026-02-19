package com.openevals4j.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.faithfulness.FaithfulnessMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FaithfulnessMetricTest {

    private FaithfulnessMetric faithfulnessMetric;

    @BeforeEach
    void setUp() {
        ChatLanguageModel chatModel =
                GoogleAiGeminiChatModel.builder()
                        .apiKey("REPLACE_YOUR_API_KEY_HERE")
                        .modelName("gemini-1.5-flash")
                        .logRequestsAndResponses(true)
                        .build();
        faithfulnessMetric =
                FaithfulnessMetric.builder().evaluatorLLM(chatModel).objectMapper(new ObjectMapper()).build();
    }

    @Test
    @Disabled("OpenAI and Gemini keys are not free")
    void evaluate() {
        EvaluationResult evaluationResult =
                faithfulnessMetric.evaluate(
                        EvaluationContext.builder()
                                .userInput("When was the first super bowl?")
                                .actualResponse("The first superbowl was held on January 15, 1968")
                                .retrievedContexts(
                                        List.of(
                                                "The First AFL–NFL World Championship Game was an American football game played on January 15, 1968, at the Los Angeles Memorial Coliseum in Los Angeles."))
                                .build());

        Assertions.assertEquals(4.0, evaluationResult.getScore());
    }

    @Test
    @Disabled("OpenAI and Gemini keys are not free")
    void evaluateBatch() {
        List<EvaluationContext> input =
                List.of(
                        EvaluationContext.builder()
                                .userInput("When was the first super bowl?")
                                .actualResponse("The first superbowl was held on January 15, 1968")
                                .retrievedContexts(
                                        List.of(
                                                "The First AFL–NFL World Championship Game was an American football game played on January 15, 1968, at the Los Angeles Memorial Coliseum in Los Angeles."))
                                .build(),
                        EvaluationContext.builder()
                                .userInput("When was the first super bowl?")
                                .actualResponse("The first superbowl was held on January 15, 1970")
                                .retrievedContexts(
                                        List.of(
                                                "The First AFL–NFL World Championship Game was an American football game played on January 15, 1968, at the Los Angeles Memorial Coliseum in Los Angeles."))
                                .build());

        List<EvaluationResult> evaluationResult = faithfulnessMetric.evaluateBatch(input);

        Assertions.assertEquals(4.0, evaluationResult.get(0).getScore());
        Assertions.assertEquals(1.0, evaluationResult.get(1).getScore());
    }
}
