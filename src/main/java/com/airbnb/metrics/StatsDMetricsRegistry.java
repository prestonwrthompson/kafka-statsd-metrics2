package com.airbnb.metrics;

import java.util.*;

import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

public class StatsDMetricsRegistry {
  private final Map<MetricName, Metric> metrics;

  public StatsDMetricsRegistry() {
    metrics = new HashMap<MetricName, Metric>();
  }

  public void register(Metric metric) {
    metrics.put(metric.metricName(), metric);
  }

  public void unregister(Metric metric) {
    metrics.remove(metric.metricName());
  }

  public Map<MetricName, Metric> getMetrics() {
    return metrics;
  }
}
