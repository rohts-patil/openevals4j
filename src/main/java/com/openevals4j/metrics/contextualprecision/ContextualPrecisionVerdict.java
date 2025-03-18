package com.openevals4j.metrics.contextualprecision;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContextualPrecisionVerdict {
  private String verdict;
  private String reason;
}
