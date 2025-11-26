# OpenEvals4J

OpenEvals4J is a Java library for evaluating RAG (Retrieval-Augmented Generation) systems and AI agents using LLM as
judge. It provides a flexible and extensible framework for assessing the performance of your RAG and agent
implementations.

## Features

- Evaluation using LLM-as-a-judge technique
- Batch evaluation capabilities
- Extensible evaluation framework
- Detailed evaluation results with explanations

## Getting Started

### Maven Dependency

Published on Maven Central https://central.sonatype.com/artifact/io.github.rohts-patil/openevals4j

```xml

<dependency>
    <groupId>io.github.rohts-patil</groupId>
    <artifactId>openevals4j</artifactId>
    <version>0.0.4</version>
</dependency>
```

### Basic Usage

```java

// Initialize your LLM
ChatLanguageModel chatModel =
        GoogleAiGeminiChatModel.builder()
                .apiKey("REPLACE_YOUR_API_KEY_HERE")
                .modelName("gemini-1.5-flash")
                .logRequestsAndResponses(true)
                .build();

// Create Faithfulness Metric's object
FaithfulnessMetric faithfulness = Faithfulness.builder().evaluatorLLM(chatModel).objectMapper(new ObjectMapper()).build();

// Evaluate the faithfulness metric
EvaluationResult evaluationResult =
        faithfulness.evaluate(
                EvaluationContext.builder()
                        .userInput("When was the first super bowl?")
                        .response("The first superbowl was held on January 15, 1968")
                        .retrievedContexts(
                                List.of(
                                        "The First AFL–NFL World Championship Game was an American football game played on January 15, 1968, at the Los Angeles Memorial Coliseum in Los Angeles."))
                        .build());

System.out.println(evaluationResult);
// EvaluationResult(score=4.0, reasoning=The answer correctly identifies the date of the first Super Bowl as January 15, 1968. However, the provided context refers to the game as the "First AFL-NFL World Championship Game", not the "Super Bowl". While the game in question is indeed the first Super Bowl, the answer's unfamiliarity with the game's original name demonstrates a lack of complete faithfulness.)
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
