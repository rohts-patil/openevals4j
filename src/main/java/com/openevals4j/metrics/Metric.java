package com.openevals4j.metrics;

import java.util.List;

public interface Metric<K, V> {

  /**
   * Computes the metric score based on the input
   *
   * @param input The input data to evaluate
   * @return A score between 0.0 and 1.0
   */
  V evaluate(K input);

  List<V> evaluateBatch(List<K> inputs);
}
