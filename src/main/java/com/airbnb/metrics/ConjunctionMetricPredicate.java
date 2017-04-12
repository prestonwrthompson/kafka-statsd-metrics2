package com.airbnb.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import org.apache.log4j.Logger;

/**
 * A metric predicate representing the conjunction (logical and) of many metric predicates
 */
public class ConjunctionMetricPredicate implements SharedMetricPredicate {
  private final Logger logger = Logger.getLogger(getClass());

  final SharedMetricPredicate[] metricPredicates;

  public ConjunctionMetricPredicate(SharedMetricPredicate... metricPredicates) {
    this.metricPredicates = metricPredicates;
  }

  @Override
  public boolean matches(MetricName name, Metric metric) {
    boolean matches = true;
    for (SharedMetricPredicate metricPredicate : metricPredicates) {
      if (!metricPredicate.matches(name, metric)) {
        matches = false;
        break;
      }
    }
    return matches;
  }

  public boolean matches(String name, org.apache.kafka.common.Metric metric) {
    boolean matches = true;
    for (SharedMetricPredicate metricPredicate : metricPredicates) {
      if (!metricPredicate.matches(name, metric)) {
        matches = false;
        break;
      }
    }
    return matches;
  }
}