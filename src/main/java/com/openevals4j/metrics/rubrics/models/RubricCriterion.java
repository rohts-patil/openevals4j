package com.openevals4j.metrics.rubrics.models;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricCriterion {
    private String name;
    private String description;
    private double weight;
    private Map<Integer, String> scoringGuidelines;
}
