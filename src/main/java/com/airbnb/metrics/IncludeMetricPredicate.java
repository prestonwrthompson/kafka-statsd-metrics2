package com.airbnb.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * A predicate to only include metrics that match the given regex
 */
public class IncludeMetricPredicate implements SharedMetricPredicate {
  private final Logger logger = Logger.getLogger(getClass());

  final String includeRegex;
  final Pattern pattern;

  public IncludeMetricPredicate(String includeRegex) {
    this.includeRegex = includeRegex;
    this.pattern = Pattern.compile(includeRegex);
  }

  @Override
  public boolean matches(MetricName name, Metric metric) {
    String n = MetricNameFormatter.format(name);
    return matches(n);
  }

  public boolean matches(String name, org.apache.kafka.common.Metric metric) {
    return matches(name);
  }

  private boolean matches(String name) {
    boolean included = pattern.matcher(name).matches();
    if (!included) {
      if (logger.isTraceEnabled()) {
        logger.trace("Metric " + name + " is excluded");
      }
    }
    return included;
  }
}