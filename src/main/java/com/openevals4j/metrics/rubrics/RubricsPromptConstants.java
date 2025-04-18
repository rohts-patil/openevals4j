package com.openevals4j.metrics.rubrics;

public class RubricsPromptConstants {

  private RubricsPromptConstants() {}

  public static final String RUBRICS_EVALUATION_PROMPT =
      """
          ## Overview
          Evaluate the response based on the provided rubric criteria. Each criterion has specific scoring guidelines.

          ## Input Format
          The evaluation will include:
          1. **User Question**: The original query submitted by the user
          2. **Assistant's Answer**: The response generated by the assistant
          3. **Rubric Criteria**: A list of criteria with scoring guidelines

          ## Rubric Format
          Each rubric criterion includes:
          - **Name**: The name of the criterion
          - **Description**: What aspect of the response this criterion evaluates
          - **Scoring Guidelines**: Specific guidelines for scoring this criterion on a scale of 1-5

          ## Evaluation Process
          For each criterion in the rubric:
          - Carefully read the criterion description and scoring guidelines
          - Evaluate the response against the criterion
          - Assign a score from 1-5 based on the scoring guidelines
          - Provide a brief justification for the score

          ## Output Format
          Provide your response in the following JSON format:
          ```json
          {
            "criteriaScores": [
              {
                "criterion": <criterion_name>,
                "score": <score_1_to_5>,
                "justification": <brief_justification>
              },
              ...
            ]
          }
          ```

          ## Rubric Criteria
          %s

          ## User Question
          %s

          ## Assistant's Answer
          %s
          """;
}
