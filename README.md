# OpenEvals4J

A Java library for evaluating Large Language Models (LLMs) applications, inspired by the OpenEvals project. This library provides a foundation for creating custom evaluation frameworks for your LLM applications.

## Features

- Base evaluation framework for LLM applications
- Support for custom evaluation metrics
- Easy integration with existing Java applications
- Java 17 compatibility
- Maven-based project structure

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.openevals4j</groupId>
    <artifactId>openevals4j</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic Example

```java
import com.github.openevals4j.eval.Evaluator;
import com.github.openevals4j.eval.metrics.AccuracyMetric;

// Create an evaluator
Evaluator evaluator = new Evaluator();

// Add evaluation metrics
evaluator.addMetric(new AccuracyMetric());

// Run evaluation
EvaluationResult result = evaluator.evaluate(testCases);
```

### Creating Custom Metrics

```java
import com.github.openevals4j.eval.metrics.Metric;

public class CustomMetric implements Metric {
    @Override
    public double evaluate(TestCase testCase) {
        // Implement your custom evaluation logic
        return score;
    }
}
```



## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.