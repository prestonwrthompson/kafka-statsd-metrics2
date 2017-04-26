package com.airbnb.metrics;

import com.timgroup.statsd.StatsDClient;
import com.yammer.metrics.core.Clock;
import kafka.utils.VerifiableProperties;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Properties;

import static org.mockito.Mockito.verify;

public class KafkaStatsDReporterTest {
  @Mock
  private Clock clock;
  @Mock
  private StatsDClient statsD;
  private KafkaStatsDReporter reporter;
  private StatsDMetricsRegistry registry;
  private Metric metric;
  private final double value = 10.11;

  @Before
  public void init() throws Exception {
    MockitoAnnotations.initMocks(this);
    StatsDReporterConfig config = new StatsDReporterConfig(new VerifiableProperties());
    registry = new StatsDMetricsRegistry();
    reporter = new KafkaStatsDReporter(statsD, registry, config);

    metric = new Metric() {
      @Override
      public MetricName metricName() {
        return new MetricName("test-metric", "group", "description", "tagkey1", "valuekey1", "tagkey2", "valuekey2");
      }

      @Override
      public double value() {
        return value;
      }
    };
  }

  protected void addMetricAndRunReporter(Metric metric) throws Exception {
    try {
      registry.register(metric);
      reporter.run();
    } finally {
      reporter.shutdown();
    }
  }

  @Test
  public final void sendDoubleGaugeWithTagsEnabled() throws Exception {
    addMetricAndRunReporter(metric);
    verify(statsD).gauge(Matchers.eq("kafka.group.test-metric"), Matchers.eq(value), Matchers.eq("tagkey1:valuekey1,tagkey2:valuekey2"));
  }

  @Test
  public final void sendDoubleGaugeWithTagsDisabled() throws Exception {
    Properties properties = new Properties();
    properties.put(StatsDReporterConfig.CONFIG_STATSD_TAG_ENABLED, "false");
    StatsDReporterConfig config = new StatsDReporterConfig(new VerifiableProperties(properties));
    reporter = new KafkaStatsDReporter(statsD, registry, config);

    addMetricAndRunReporter(metric);
    verify(statsD).gauge(Matchers.eq("kafka.group.tagkey1.valuekey1.tagkey2.valuekey2.test-metric"), Matchers.eq(value));
  }
}
