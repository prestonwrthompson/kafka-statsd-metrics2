package com.airbnb.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * A metric predicate representing the conjunction (logical and) of many metric predicates
 */
public class ConjunctionMetricPredicate implements MetricPredicate {
  private final Logger logger = Logger.getLogger(getClass());

  final MetricPredicate[] metricPredicates;

  public ConjunctionMetricPredicate(MetricPredicate... metricPredicates) {
    this.metricPredicates = metricPredicates;
  }

  @Override
  public boolean matches(MetricName name, Metric metric) {
    boolean matches = true;
    for (MetricPredicate metricPredicate : metricPredicates) {
      if (!metricPredicate.matches(name, metric)) {
        matches = false;
        break;
      }
    }

    if (!matches) {
      if (logger.isTraceEnabled()) {
        logger.trace("Metric " + name + " is excluded");
      }
    }
    return matches;
  }
}
