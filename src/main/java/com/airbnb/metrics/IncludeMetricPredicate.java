package com.airbnb.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;
import org.apache.log4j.Logger;

import java.util.regex.Pattern;

/**
 * A predicate to only include metrics that match the given regex
 */
public class IncludeMetricPredicate implements MetricPredicate {
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
    boolean included = pattern.matcher(n).matches();
    if (!included) {
      if (logger.isTraceEnabled()) {
        logger.trace("Metric " + n + " is excluded");
      }
    }
    return included;
  }
}
