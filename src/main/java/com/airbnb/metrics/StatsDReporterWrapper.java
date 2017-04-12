package com.airbnb.metrics;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import com.yammer.metrics.Metrics;
import kafka.utils.VerifiableProperties;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is a wrapper around the StatsDReporter that handles creating, configuring, starting and stopping the
 * StatsDReporter. It provides a common interface so that metrics reporters that implement KafkaMetricsReporter
 * and those that implement MetricReporter can share common functionality.
 */
public class StatsDReporterWrapper {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsDReporterWrapper.class);

  private final String reporterName;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private StatsDReporter underlying = null;
  private StatsDReporterConfig statsDReporterConfig;

  public StatsDReporterWrapper(String reporterName) {
    this.reporterName = reporterName;
  }

  public void configure(VerifiableProperties properties) {
    statsDReporterConfig = new StatsDReporterConfig(properties);
  }

  public void startReporter(long pollingPeriodInSeconds) {
    if (pollingPeriodInSeconds <= 0) {
      throw new IllegalArgumentException("Polling period must be greater than zero");
    }

    if (!statsDReporterConfig.isEnabled()) {
      log.info("Reporter is disabled");
    } else {
      synchronized (running) {
        if (running.get()) {
          log.warn("Reporter is already running");
        } else {
          StatsDClient statsDClient = createStatsDClient();
          underlying = new StatsDReporter(Metrics.defaultRegistry(), statsDClient, statsDReporterConfig, reporterName);
          underlying.start(pollingPeriodInSeconds, TimeUnit.SECONDS);
          log.info("Started Reporter with host={}, port={}, polling_period_secs={}, prefix={}",
            statsDReporterConfig.getHost(), statsDReporterConfig.getPort(), pollingPeriodInSeconds,
            statsDReporterConfig.getPrefix()
          );
          running.set(true);
        }
      }
    }
  }

  // when the reporter is started without a specified polling period, use the one provided by the config
  public void startReporter() {
    startReporter(statsDReporterConfig.getPollingPeriodInSeconds());
  }

  public void stopReporter() {
    if (!statsDReporterConfig.isEnabled()) {
      log.warn("Reporter is disabled");
    } else {
      synchronized (running) {
        if (running.get()) {
          underlying.shutdown();
          running.set(false);
          log.info("Stopped Reporter with host={}, port={}", statsDReporterConfig.getHost(), statsDReporterConfig.getPort());
        } else {
          log.warn("Reporter is not running");
        }
      }
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  private StatsDClient createStatsDClient() {
    try {
      return new NonBlockingStatsDClient(statsDReporterConfig.getPrefix(), statsDReporterConfig.getHost(), statsDReporterConfig.getPort());
    } catch (StatsDClientException ex) {
      log.error("Statsd Client cannot be started");
      throw ex;
    }
  }
}
