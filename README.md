<h1 align="center">OpenEvals4J</h1>

<p align="center">
  <b>A flexible and extensible Java library for evaluating RAG systems and AI agents using LLM-as-a-Judge.</b>
</p>

## Overview

OpenEvals4J is an evaluation framework built on top of [LangChain4j](https://github.com/langchain4j/langchain4j), designed specifically for Java developers. It provides a robust, LLM-driven approach to measure the quality of Retrieval-Augmented Generation (RAG) pipelines and conversational agents.

## Features

- 🧠 **LLM-as-a-Judge**: Leverage the reasoning capabilities of state-of-the-art LLMs (via LangChain4j) to evaluate text generations.
- 📊 **Rich Evaluation Results**: Every evaluation provides a numeric `score` (typically 1.0 to 5.0) alongside detailed text `reasoning`.
- 🧩 **Comprehensive Metrics Suite**: Evaluate various aspects of your RAG pipeline out-of-the-box.
- 🛠️ **Customizable Rubrics**: Easily define your own evaluation criteria using `RubricsBasedMetric`.
- ⚡ **Batch Evaluation**: Interface designed to support batch evaluation of multiple contexts at once.

## Supported Metrics

- **Faithfulness**: Measures if the generated answer is factually accurate and derived exclusively from the retrieved context.
- **Contextual Precision**: Evaluates whether all of the ground-truth relevant items in the retrieved context are ranked highly in the retrieved list.
- **Contextual Recall**: Evaluates the extent to which the retrieved context aligns with the expected baseline/ground-truth.
- **Contextual Relevancy**: Assesses how relevant the retrieved context is to the original user query.
- **Response Completeness**: Checks if the response completely addresses all aspects of the user's question.
- **Rubrics-Based**: A highly customizable metric that scores the response against user-defined criteria and weights.

## Installation

Add the following dependency to your project. OpenEvals4J is available on Maven Central.

### Maven

```xml
<dependency>
    <groupId>io.github.rohts-patil</groupId>
    <artifactId>openevals4j</artifactId>
    <version>0.0.6</version> 
</dependency>
```

### Gradle

```groovy
implementation 'io.github.rohts-patil:openevals4j:0.0.6'
```

*Note: Make sure you also include your desired LangChain4j model implementation (e.g., `langchain4j-google-ai-gemini`, `langchain4j-open-ai`).*

## Getting Started

Here is a simple example showing how to evaluate the **Faithfulness** of an assistant's response.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.faithfulness.FaithfulnessMetric;
import com.openevals4j.metrics.models.EvaluationContext;
import com.openevals4j.metrics.models.EvaluationResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.List;

public class BasicUsageExample {
    public static void main(String[] args) {
        // 1. Initialize your LLM evaluator (via LangChain4j)
        ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(System.getenv("GEMINI_API_KEY"))
                .modelName("gemini-2.5-flash")
                .logRequestsAndResponses(true)
                .build();

        // 2. Instantiate the metric
        FaithfulnessMetric faithfulness = FaithfulnessMetric.builder()
                .evaluatorLLM(chatModel)
                .objectMapper(new ObjectMapper())
                .build();

        // 3. Create the Evaluation Context
        EvaluationContext context = EvaluationContext.builder()
                .userInput("When was the first super bowl?")
                .actualResponse("The first superbowl was held on January 15, 1968")
                .retrievedContexts(List.of(
                    "The First AFL–NFL World Championship Game was an American football game played on January 15, 1968, at the Los Angeles Memorial Coliseum in Los Angeles."
                ))
                .build();

        // 4. Evaluate
        EvaluationResult result = faithfulness.evaluate(context);

        // 5. Review the result
        System.out.println("Score: " + result.getScore());
        System.out.println("Reasoning: " + result.getReasoning());
    }
}
```

## Using the Metric Factory

Instead of instantiating each metric explicitly, you can use the `LLMBasedMetricFactory`.

```java
import com.openevals4j.metrics.LLMBasedMetricFactory;
import com.openevals4j.metrics.MetricName;
import com.openevals4j.metrics.LLMBasedMetric;

LLMBasedMetric<EvaluationContext, EvaluationResult> completenessMetric = 
    LLMBasedMetricFactory.createMetric(
        MetricName.RESPONSE_COMPLETENESS,
        chatModel,
        new ObjectMapper(),
        null // rubricCriteria (only required for RUBRICS_BASED)
    );

EvaluationResult result = completenessMetric.evaluate(context);
```

## Creating Custom Rubrics

If you need a specific evaluation not covered by standard metrics, you can use `RubricsBasedMetric`.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openevals4j.metrics.rubrics.RubricsBasedMetric;
import com.openevals4j.metrics.rubrics.models.RubricCriterion;
import java.util.List;
import java.util.Map;

List<RubricCriterion> criteria = List.of(
    RubricCriterion.builder()
        .name("Politeness")
        .description("Evaluates if the response is polite and respectful.")
        .weight(1.0)
        .scoringGuidelines(Map.of(
            1, "Rude or condescending tone.",
            3, "Neutral, straightforward tone without being overly polite.",
            5, "Very polite, respectful, and appropriately professional."
        ))
        .build()
);

RubricsBasedMetric politeMetric = RubricsBasedMetric.builder()
    .evaluatorLLM(chatModel)
    .objectMapper(new ObjectMapper())
    .rubricCriteria(criteria)
    .build();

EvaluationResult result = politeMetric.evaluate(context);
```

## Requirements
- Java 17 or higher
- SLF4J (for logging)
- Jackson (for JSON parsing)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an Issue.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
