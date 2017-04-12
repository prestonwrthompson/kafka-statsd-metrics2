package com.airbnb.metrics;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.timgroup.statsd.StatsDClient;
import org.apache.kafka.common.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaStatsDReporter implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(KafkaStatsDReporter.class);
  private final ScheduledExecutorService executor;

  private final StatsDClient statsDClient;
  private final StatsDMetricsRegistry registry;
  private StatsDReporterConfig statsDReporterConfig;

  public KafkaStatsDReporter(StatsDClient statsDClient, StatsDMetricsRegistry registry, StatsDReporterConfig config) {
    this.statsDClient = statsDClient;
    this.registry = registry;
    this.executor = new ScheduledThreadPoolExecutor(1);
    this.statsDReporterConfig = config;
  }

  public void start(long period, TimeUnit unit) {
    executor.scheduleWithFixedDelay(this, period, period, unit);
  }

  public void shutdown() throws InterruptedException {
    executor.shutdown();
  }

  private void sendAllKafkaMetrics() {
    for (String metricName : registry.getMetricsName()) {
      sendAMetric(metricName);
    }
  }

  private void sendAMetric(String metricName) {
    Metric metric = registry.getMetric(metricName);
    String tag = registry.getTag(metricName);

    if (metric != null && statsDReporterConfig.getMetricPredicate().matches(metricName, metric)) {
      final Object value = metric.value();
      Double val = new Double(value.toString());

      if (val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY) {
        val = 0D;
      }

      if (statsDReporterConfig.isTagEnabled() && tag != null) {
        statsDClient.gauge(metricName, val, tag);
      } else {
        statsDClient.gauge(metricName, val);
      }
    }

  }

  @Override
  public void run() {
    sendAllKafkaMetrics();
  }
}
