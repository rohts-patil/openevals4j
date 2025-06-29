package com.openevals4j.metrics.responsecompleteness;

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

class ResponseCompletenessMetricTest {

  private ResponseCompletenessMetric responseCompletenessMetric;

  @BeforeEach
  void setUp() {
    ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
            .apiKey("REPLACE_YOUR_API_KEY_HERE")
            .modelName("gemini-1.5-flash")
            .logRequestsAndResponses(true)
            .build();
    responseCompletenessMetric =
        ResponseCompletenessMetric.builder()
            .evaluatorLLM(chatModel)
            .objectMapper(new ObjectMapper())
            .build();
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_CompleteResponse() {
    // This test verifies that responses covering all essential concepts score well
    // even if they use slightly different terminology or less detail than expected
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput(
                    "Explain what machine learning is and provide two examples of its applications.")
                .expectedResponse(
                    "Machine learning is a subset of artificial intelligence that enables computers to learn and make decisions from data without being explicitly programmed. Two examples of its applications are: 1) Email spam detection - algorithms learn to identify spam emails based on patterns in email content and metadata, and 2) Recommendation systems - platforms like Netflix and Amazon use ML to suggest content or products based on user behavior and preferences.")
                .actualResponse(
                    "Machine learning is a subset of AI that allows computers to learn from data without explicit programming. Two examples are: 1) Email spam detection where algorithms identify spam based on email patterns, and 2) Recommendation systems like those used by Netflix to suggest movies based on viewing history.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score high (3-5) as it covers all main concepts: definition and two examples
    // Even if some details are expressed differently, the core concepts are present
    Assertions.assertTrue(evaluationResult.getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_IncompleteResponse() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput(
                    "Explain what machine learning is and provide two examples of its applications.")
                .expectedResponse(
                    "Machine learning is a subset of artificial intelligence that enables computers to learn and make decisions from data without being explicitly programmed. Two examples of its applications are: 1) Email spam detection - algorithms learn to identify spam emails based on patterns in email content and metadata, and 2) Recommendation systems - platforms like Netflix and Amazon use ML to suggest content or products based on user behavior and preferences.")
                .actualResponse(
                    "Machine learning is a subset of AI that allows computers to learn from data.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score low (1-2) as it only covers the definition but misses the examples
    Assertions.assertTrue(evaluationResult.getScore() <= 2.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_PartiallyCompleteResponse() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What are the benefits of renewable energy?")
                .expectedResponse(
                    "Renewable energy offers several benefits: 1) Environmental benefits - reduces greenhouse gas emissions and pollution, 2) Economic benefits - creates jobs and reduces energy costs over time, 3) Energy security - reduces dependence on fossil fuel imports.")
                .actualResponse(
                    "Renewable energy reduces greenhouse gas emissions and creates jobs.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score moderately (2-4) as it covers some but not all benefits
    Assertions.assertTrue(evaluationResult.getScore() >= 2.0 && evaluationResult.getScore() <= 4.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_ExceedsExpectedScope() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What is photosynthesis?")
                .expectedResponse(
                    "Photosynthesis is the process by which plants convert sunlight, carbon dioxide, and water into glucose and oxygen.")
                .actualResponse(
                    "Photosynthesis is the biological process where plants, algae, and some bacteria convert light energy into chemical energy. Plants use sunlight, carbon dioxide from air, and water from soil to produce glucose (sugar) and release oxygen as a byproduct. This process occurs in chloroplasts using chlorophyll and involves two main stages: light-dependent reactions and the Calvin cycle.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score high (4-5) as it covers all expected elements and more
    Assertions.assertTrue(evaluationResult.getScore() >= 4.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluateBatch() {
    List<EvaluationContext> input =
        List.of(
            EvaluationContext.builder()
                .userInput("What are the benefits of renewable energy?")
                .expectedResponse(
                    "Renewable energy offers several benefits: 1) Environmental benefits - reduces greenhouse gas emissions and pollution, 2) Economic benefits - creates jobs and reduces energy costs over time, 3) Energy security - reduces dependence on fossil fuel imports.")
                .actualResponse(
                    "Renewable energy reduces greenhouse gas emissions, creates jobs, and provides energy security by reducing fossil fuel dependence.")
                .build(),
            EvaluationContext.builder()
                .userInput("What are the benefits of renewable energy?")
                .expectedResponse(
                    "Renewable energy offers several benefits: 1) Environmental benefits - reduces greenhouse gas emissions and pollution, 2) Economic benefits - creates jobs and reduces energy costs over time, 3) Energy security - reduces dependence on fossil fuel imports.")
                .actualResponse("Renewable energy is good for the environment.")
                .build(),
            EvaluationContext.builder()
                .userInput("Explain the water cycle.")
                .expectedResponse(
                    "The water cycle is the continuous movement of water through evaporation, condensation, precipitation, and collection.")
                .actualResponse(
                    "The water cycle involves water evaporating from oceans, forming clouds through condensation, falling as precipitation, and collecting in bodies of water to repeat the process.")
                .build());

    List<EvaluationResult> evaluationResults = responseCompletenessMetric.evaluateBatch(input);

    Assertions.assertEquals(3, evaluationResults.size());

    for (EvaluationResult result : evaluationResults) {
      Assertions.assertTrue(result.getScore() >= 1.0 && result.getScore() <= 5.0);
      Assertions.assertNotNull(result.getReasoning());
    }

    // First response should score higher than second (covers more aspects)
    Assertions.assertTrue(
        evaluationResults.get(0).getScore() > evaluationResults.get(1).getScore());

    // Second response should score low as it only covers environmental benefits
    Assertions.assertTrue(evaluationResults.get(1).getScore() <= 3.0);

    // Third response should score well as it covers all main aspects
    Assertions.assertTrue(evaluationResults.get(2).getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_DifferentButValidApproach() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("How do you make a peanut butter sandwich?")
                .expectedResponse(
                    "To make a peanut butter sandwich: 1) Get two slices of bread, 2) Spread peanut butter on one slice, 3) Optionally add jelly to the other slice, 4) Put the slices together.")
                .actualResponse(
                    "Start by gathering bread, peanut butter, and optionally jelly. Take one slice and apply peanut butter evenly. If using jelly, spread it on the second slice. Combine the slices with the spreads facing inward to complete your sandwich.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score well as it covers all essential steps despite different organization
    Assertions.assertTrue(evaluationResult.getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_ConciseButComplete() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What is gravity?")
                .expectedResponse(
                    "Gravity is a fundamental force of nature that attracts objects with mass toward each other. It keeps us on Earth and governs planetary motion.")
                .actualResponse(
                    "Gravity is a force that attracts objects with mass, keeping us on Earth and controlling planetary orbits.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score well as it efficiently covers all key points
    Assertions.assertTrue(evaluationResult.getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_ComplexTechnicalTopic() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("Explain how blockchain technology works.")
                .expectedResponse(
                    "Blockchain is a distributed ledger technology that maintains a continuously growing list of records (blocks) linked using cryptography. Each block contains a cryptographic hash of the previous block, a timestamp, and transaction data. The decentralized nature makes it resistant to modification and provides transparency.")
                .actualResponse(
                    "Blockchain is a digital ledger that stores data in blocks connected by cryptographic hashes. Each block references the previous one, creating a chain. It's decentralized across multiple computers, making it secure and transparent since no single entity controls it.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score well as it covers the main technical concepts
    Assertions.assertTrue(evaluationResult.getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_MultiPartQuestion() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What is climate change and what are its main causes and effects?")
                .expectedResponse(
                    "Climate change refers to long-term shifts in global temperatures and weather patterns. Main causes include burning fossil fuels, deforestation, and industrial activities that release greenhouse gases. Effects include rising sea levels, extreme weather events, ecosystem disruption, and threats to food security.")
                .actualResponse(
                    "Climate change is the long-term alteration of Earth's climate patterns. It's primarily caused by human activities like burning fossil fuels and deforestation, which increase greenhouse gases. This leads to rising temperatures, melting ice caps, more frequent storms, and impacts on agriculture and wildlife.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score high as it addresses definition, causes, and effects
    Assertions.assertTrue(evaluationResult.getScore() >= 3.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_PartialMultiPartResponse() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What is climate change and what are its main causes and effects?")
                .expectedResponse(
                    "Climate change refers to long-term shifts in global temperatures and weather patterns. Main causes include burning fossil fuels, deforestation, and industrial activities that release greenhouse gases. Effects include rising sea levels, extreme weather events, ecosystem disruption, and threats to food security.")
                .actualResponse(
                    "Climate change is the long-term alteration of Earth's climate patterns. It's primarily caused by human activities like burning fossil fuels and deforestation.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score moderately as it covers definition and causes but misses effects
    Assertions.assertTrue(evaluationResult.getScore() >= 2.0 && evaluationResult.getScore() <= 4.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_VeryBriefResponse() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("Explain the process of photosynthesis.")
                .expectedResponse(
                    "Photosynthesis is the process by which plants convert sunlight, carbon dioxide, and water into glucose and oxygen using chlorophyll in their leaves.")
                .actualResponse("Plants make food from sunlight.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score low as it's overly simplified and misses key details
    Assertions.assertTrue(evaluationResult.getScore() <= 2.0);
  }

  @Test
  @Disabled("OpenAI and Gemini keys are not free")
  void evaluate_OffTopicResponse() {
    EvaluationResult evaluationResult =
        responseCompletenessMetric.evaluate(
            EvaluationContext.builder()
                .userInput("What are the benefits of exercise?")
                .expectedResponse(
                    "Exercise provides numerous benefits including improved cardiovascular health, stronger muscles, better mental health, weight management, and increased energy levels.")
                .actualResponse(
                    "Diet is very important for maintaining good health. You should eat plenty of fruits and vegetables.")
                .build());

    Assertions.assertTrue(evaluationResult.getScore() >= 1.0 && evaluationResult.getScore() <= 5.0);
    Assertions.assertNotNull(evaluationResult.getReasoning());
    // Should score very low as it doesn't address the question about exercise
    Assertions.assertTrue(evaluationResult.getScore() <= 2.0);
  }
}
