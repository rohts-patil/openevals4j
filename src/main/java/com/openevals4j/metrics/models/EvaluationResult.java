package com.openevals4j.metrics.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult {

  private double score;

  private String reasoning;

  private Map<String, Object> debugData;
}
