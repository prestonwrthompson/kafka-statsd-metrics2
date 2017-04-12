package com.airbnb.metrics;

import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricPredicate;

/**
 * Created by preston on 4/12/17.
 */
public interface SharedMetricPredicate extends MetricPredicate {
  /**
   * A predicate which matches all inputs.
   */
  SharedMetricPredicate ALL = new SharedMetricPredicate() {
    @Override
    public boolean matches(MetricName name, Metric metric) {
      return true;
    }

    public boolean matches(String name, org.apache.kafka.common.Metric metric) {
      return true;
    }
  };

  /**
   * Returns {@code true} if the metric matches the predicate.
   *
   * @param name   the name of the metric
   * @param metric the metric itself
   * @return {@code true} if the predicate applies, {@code false} otherwise
   */
  boolean matches(MetricName name, Metric metric);

  boolean matches(String name, org.apache.kafka.common.Metric metric);

}
