package com.openevals4j.metrics.context;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationContext {
  private String userInput;
  private String response;
  private List<String> retrievedContexts;
  private List<String> referenceContexts;
  private Object metadata;
}
