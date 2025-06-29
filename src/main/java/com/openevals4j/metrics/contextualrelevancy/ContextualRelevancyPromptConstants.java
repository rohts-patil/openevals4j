package com.openevals4j.metrics.contextualrelevancy;

public class ContextualRelevancyPromptConstants {

  private ContextualRelevancyPromptConstants() {}

  public static final String VERDICT_GENERATION_PROMPT =
"""
Given the user input and retrieval context, please generate a list of JSON objects to determine whether each node in the retrieval context is relevant to answering the user's question.

**
IMPORTANT: Please make sure to only return in JSON format, with the list of JSON. These JSON only contain the `verdict` key that outputs only 'yes' or 'no', and a `reason` key to justify the verdict. In your reason, you should aim to quote parts of the context.

Example Retrieval Context: ["The capital of France is Paris.", "Paris is known for the Eiffel Tower.", "The weather today is sunny."]
Example Input: "What is the capital of France?"

Example:

    [
        {
            "verdict": "yes",
            "reason": "This directly answers the question by stating 'The capital of France is Paris.'"
        },
        {
            "verdict": "yes",
            "reason": "This provides additional relevant information about Paris, which is the capital mentioned in the question."
        },
        {
            "verdict": "no",
            "reason": "'The weather today is sunny' is not relevant to the question about France's capital."
        }
    ]

Since you are going to generate a verdict for each context, the number of 'verdicts' SHOULD BE STRICTLY EQUAL to that of the contexts.

## Evaluation Criteria
A context node is relevant if it:
- Directly answers or helps answer the user's question
- Provides supporting information related to the topic
- Contains facts, definitions, or explanations that are useful for the query
- Offers background information that enhances understanding of the topic

A context node is NOT relevant if it:
- Discusses completely unrelated topics
- Contains information that doesn't help answer the question
- Provides irrelevant details that don't contribute to understanding
- Is off-topic or tangential to the user's query
**

Input:
%s

Retrieval Context %s:
%s
""";

  public static final String REASON_GENERATION_PROMPT =
"""
Given the user input, retrieval contexts, and contextual relevancy score, provide a CONCISE summary for the score. Explain what makes the score what it is.

The retrieval contexts is a list of JSON with `verdict` and `reason` keys. `verdict` will be either 'yes' or 'no', which represents whether the corresponding node in the retrieval context is relevant to the user's input.

Contextual relevancy measures the proportion of retrieved contexts that are actually relevant to answering the user's question. A higher score means more of the retrieved contexts are useful and relevant.

**
IMPORTANT: Please make sure to only return in JSON format, with the 'reason' key providing the reason.
Example JSON:
{
    "reason": "The score is <contextual_relevancy_score> because <your_reason>."
}

DO NOT mention 'verdict' in your reason, but instead phrase it as relevant/irrelevant nodes. The term 'verdict' is just here for you to understand the broader scope of things.
Also DO NOT mention there are `reason` fields in the retrieval contexts you are presented with, instead just use the information in the `reason` field.
In your reason, you MUST USE the information from the `reason` field to explain why certain nodes are relevant or irrelevant to the user's question.
When addressing nodes, make it explicit that they are nodes in retrieval contexts.
If the score is 1, keep it positive and mention that all retrieved contexts are relevant.
If the score is 0, explain that none of the retrieved contexts help answer the user's question.
**

Contextual Relevancy Score:
%s

Input:
%s

Retrieval Contexts:
%s

JSON:
""";
}
