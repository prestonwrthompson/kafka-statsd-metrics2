package com.airbnb.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.timgroup.statsd.StatsDClient;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaStatsDReporter implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(KafkaStatsDReporter.class);

  private static final String METRIC_PREFIX = "kafka.";

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

  @Override
  public void run() {
    sendAllKafkaMetrics();
  }

  private void sendAllKafkaMetrics() {
    final Map<MetricName, Metric> allMetrics = new HashMap<MetricName, Metric>(registry.getMetrics());
    for (Map.Entry<MetricName, Metric> entry : allMetrics.entrySet()) {
      sendAMetric(entry.getKey(), entry.getValue());
    }
  }

  private void sendAMetric(MetricName metricName, Metric metric) {
    String gaugeMetricName = getGaugeMetricName(metricName);
//    String tagSuffix = getTagSuffix(metricName);

    if (metric != null && statsDReporterConfig.getMetricPredicate().matches(gaugeMetricName, metric)) {
      final Object value = metric.value();
      Double val = new Double(value.toString());

      if (val == Double.NEGATIVE_INFINITY || val == Double.POSITIVE_INFINITY) {
        val = 0D;
      }

      if (statsDReporterConfig.isTagEnabled()) {
        statsDClient.gauge(gaugeMetricName, val, getTagSuffix(metricName));
      } else {
        statsDClient.gauge(gaugeMetricName, val);
      }
    }
  }

  private String getGaugeMetricName(MetricName metricName) {
    StringBuilder stringBuilder = new StringBuilder(METRIC_PREFIX).append(metricName.group());

    // if datadog tags are not enabled, include the tag keys and values in the gauge metric name
    if (!statsDReporterConfig.isTagEnabled()) {
      for(Map.Entry<String, String>  entry : metricName.tags().entrySet()) {
        stringBuilder.append(".").append(entry.getKey()).append(".").append(entry.getValue());
      }
    }

    return stringBuilder.append(".").append(metricName.name()).toString();
  }

  private String getTagSuffix(MetricName metricName) {
    StringBuilder strBuilder = new StringBuilder();

    for (String key : metricName.tags().keySet()) {
      strBuilder.append(key).append(":").append(metricName.tags().get(key)).append(",");
    }

    if (strBuilder.length() > 0) {
      strBuilder.deleteCharAt(strBuilder.length() - 1);
    }

    return strBuilder.toString();
  }
}
