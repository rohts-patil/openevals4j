package com.openevals4j.metrics.responsecompleteness;

public class ResponseCompletenessPromptConstants {

  private ResponseCompletenessPromptConstants() {}

  public static final String RESPONSE_COMPLETENESS_EVALUATION_PROMPT =
      """
          ## Overview
          Evaluate the completeness of an assistant's response based on how thoroughly it addresses all aspects, requirements, and sub-questions present in the user's input, using the expected response as a reference for what constitutes a complete answer.

          ## Input Format
          The evaluation will include:
          1. **User Question/Request**: The original query or request submitted by the user
          2. **Expected Response**: The ground truth or reference response that represents a complete answer
          3. **Assistant's Response**: The response generated by the assistant

          ## Evaluation Scale (1-5)
          | Score | Description | Criteria |
          |-------|-------------|----------|
          | 1 | **Severely Incomplete** | • Misses most essential concepts or requirements from the user's question<br>• Fails to address the main topic or provides irrelevant information<br>• Covers only peripheral aspects while ignoring core elements<br>• Provides minimal value in answering the user's question |
          | 2 | **Significantly Incomplete** | • Addresses some key concepts but misses several important elements<br>• Covers the main topic but lacks substantial supporting details or examples<br>• Provides partial understanding but leaves major gaps<br>• Answers only part of a multi-part question |
          | 3 | **Partially Complete** | • Covers most essential concepts with adequate explanation<br>• Addresses the main requirements but may lack some supporting details<br>• Provides sufficient information for basic understanding<br>• May miss some nuances or secondary aspects |
          | 4 | **Mostly Complete** | • Thoroughly addresses all essential concepts and requirements<br>• Provides good detail and explanation for key points<br>• Covers all major aspects with only minor gaps or less detail<br>• Demonstrates comprehensive understanding of the topic |
          | 5 | **Completely Comprehensive** | • Addresses all aspects of the question with excellent detail<br>• Provides thorough explanations and comprehensive coverage<br>• May include valuable additional insights beyond the expected response<br>• Demonstrates deep understanding and complete coverage |

          ## Evaluation Process
          1. **Identify Core Requirements**: Determine what the user is asking for (definition, examples, explanation, etc.)
          2. **Extract Essential Concepts**: Identify the key concepts and information needed to fully answer the question
          3. **Assess Concept Coverage**: Check if the assistant's response addresses all essential concepts, even if expressed differently
          4. **Evaluate Completeness**: Determine if the response provides sufficient information to answer the user's question
          5. **Consider Equivalent Expressions**: Recognize that concepts can be expressed in different but equivalent ways
          6. **Focus on Understanding**: Prioritize whether the response demonstrates understanding over exact wording

          ## Scoring Guidelines
          - **Focus on conceptual coverage** rather than exact detail matching
          - **Recognize equivalent expressions**: "user behavior" and "viewing history" convey the same concept
          - **Prioritize essential elements**: Core concepts matter more than specific terminology
          - **Value clear communication**: Well-explained concepts should score well even if less detailed
          - **Consider the user's needs**: Does the response provide what the user was asking for?
          - **Don't penalize conciseness**: Efficient responses that cover key points should score well
          - **Reward comprehensive understanding**: Responses showing good grasp of concepts should score highly

          ## Examples of Equivalent Expressions
          - "email content and metadata" ≈ "email patterns"
          - "user behavior and preferences" ≈ "viewing history" or "user data"
          - "artificial intelligence" ≈ "AI"
          - "without being explicitly programmed" ≈ "without explicit programming"

          ## Important Notes
          - If the assistant covers all main concepts requested by the user, score should be 4-5 even if some details differ
          - Only score 1-2 if major concepts are completely missing or the response is off-topic
          - Score 3 if the response covers most concepts but misses significant elements
          - Focus on whether someone reading the response would understand the topic adequately

          ## Output Format
          Provide your response in the following JSON format:
          {
            "score": <number between 1 and 5>,
            "reasoning": "<detailed analysis of what was covered, what was missed, and why the score was assigned>"
          }

          User Question/Request: %s

          Expected Response: %s

          Assistant's Response: %s
          """;
}
