package com.airbnb.kafka.kafka09;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.airbnb.metrics.StatsDReporterConfig;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StatsdMetricsReporterTest {
  private Map<String, String> configs;

  @Before
  public void init() {
    configs = new HashMap<String, String>();
    configs.put(StatsDReporterConfig.CONFIG_STATSD_HOST, "127.0.0.1");
    configs.put(StatsDReporterConfig.CONFIG_STATSD_PORT, "1234");
    configs.put(StatsDReporterConfig.CONFIG_STATSD_METRICS_PREFIX, "foo");
    configs.put(StatsDReporterConfig.CONFIG_STATSD_REPORTER_ENABLED, "false");
  }

  @Test
  public void init_should_start_reporter_when_enabled() {
    configs.put(StatsDReporterConfig.CONFIG_STATSD_REPORTER_ENABLED, "true");
    StatsdMetricsReporter reporter = new StatsdMetricsReporter();
    assertFalse("reporter should not be running", reporter.isRunning());
    reporter.configure(configs);
    reporter.init(new ArrayList<KafkaMetric>());
    assertTrue("reporter should be running once #init has been invoked", reporter.isRunning());
  }

  @Test
  public void init_should_not_start_reporter_when_disabled() {
    configs.put(StatsDReporterConfig.CONFIG_STATSD_REPORTER_ENABLED, "false");
    StatsdMetricsReporter reporter = new StatsdMetricsReporter();
    assertFalse("reporter should not be running", reporter.isRunning());
    reporter.configure(configs);
    reporter.init(new ArrayList<KafkaMetric>());
    assertFalse("reporter should NOT be running once #init has been invoked", reporter.isRunning());
  }
}
