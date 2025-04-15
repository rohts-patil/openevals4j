package com.openevals4j.metrics.rubrics.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a score for a specific criterion in a rubric-based evaluation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterionScore {
    /**
     * The name of the criterion being evaluated.
     */
    private String criterion;
    
    /**
     * The score assigned to the criterion (typically on a scale of 1-5).
     */
    private int score;
    
    /**
     * A justification or explanation for the assigned score.
     */
    private String justification;
}
