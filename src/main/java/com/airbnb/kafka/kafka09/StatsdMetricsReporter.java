/*
 * Copyright (c) 2015.  Airbnb.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.airbnb.kafka.kafka09;

import com.airbnb.metrics.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import kafka.utils.VerifiableProperties;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.LoggerFactory;

public class StatsdMetricsReporter implements MetricsReporter {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsDReporter.class);

  private static final String REPORTER_NAME = "kafka-statsd-metrics-0.5";

  private final AtomicBoolean running = new AtomicBoolean(false);
  private StatsDClient statsd;
  private StatsDMetricsRegistry registry;
  private KafkaStatsDReporter underlying = null;
  private StatsDReporterConfig statsDReporterConfig;

  public boolean isRunning() {
    return running.get();
  }

  @Override
  public void init(List<KafkaMetric> metrics) {
    registry = new StatsDMetricsRegistry();

    if (statsDReporterConfig.isEnabled()) {
      startReporter();
    } else {
      log.warn("KafkaStatsDReporter is disabled");
    }

    for (KafkaMetric metric : metrics) {
      metricChange(metric);
    }
  }

  @Override
  public void metricChange(final KafkaMetric metric) {
    registry.register(metric);
  }

  @Override
  public void metricRemoval(KafkaMetric metric) {
    registry.unregister(metric);
  }

  @Override
  public void close() {
    stopReporter();
  }

  @Override
  public void configure(Map<String, ?> configs) {
    Properties props = new Properties();
    for (Map.Entry<String, ?> entry : configs.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
    VerifiableProperties verifiableProperties = new VerifiableProperties(props);
    statsDReporterConfig = new StatsDReporterConfig(verifiableProperties);
  }

  private void startReporter() {
    if (statsDReporterConfig.getPollingPeriodInSeconds() <= 0) {
      throw new IllegalArgumentException("Polling period must be greater than zero");
    }

    synchronized (running) {
      if (running.get()) {
        log.warn("KafkaStatsDReporter: {} is already running", REPORTER_NAME);
      } else {
        statsd = createStatsd();
        underlying = new KafkaStatsDReporter(statsd, registry, statsDReporterConfig);
        underlying.start(statsDReporterConfig.getPollingPeriodInSeconds(), TimeUnit.SECONDS);
        log.info(
          "Started KafkaStatsDReporter: {} with host={}, port={}, polling_period_secs={}, prefix={}",
          REPORTER_NAME, statsDReporterConfig.getHost(), statsDReporterConfig.getPort(),
          statsDReporterConfig.getPollingPeriodInSeconds(), statsDReporterConfig.getPrefix()
        );
        running.set(true);
      }
    }
  }

  private StatsDClient createStatsd() {
    try {
      return new NonBlockingStatsDClient(statsDReporterConfig.getPrefix(), statsDReporterConfig.getHost(), statsDReporterConfig.getPort());
    } catch (StatsDClientException e) {
      log.error("KafkaStatsDReporter cannot be started");
      throw e;
    }
  }

  private void stopReporter() {
    if (!statsDReporterConfig.isEnabled()) {
      log.warn("KafkaStatsDReporter is disabled");
    } else {
      synchronized (running) {
        if (running.get()) {
          try {
            underlying.shutdown();
          } catch (InterruptedException e) {
            log.warn("Stop reporter exception: {}", e);
          }
          statsd.stop();
          running.set(false);
          log.info("Stopped KafkaStatsDReporter with host={}, port={}", statsDReporterConfig.getHost(), statsDReporterConfig.getPort());
        } else {
          log.warn("KafkaStatsDReporter is not running");
        }
      }
    }
  }
}
