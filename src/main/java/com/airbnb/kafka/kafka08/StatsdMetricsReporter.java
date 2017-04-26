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

package com.airbnb.kafka.kafka08;

import com.airbnb.metrics.StatsDReporter;
import com.airbnb.metrics.StatsDReporterConfig;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientException;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class StatsdMetricsReporter implements StatsdMetricsReporterMBean, KafkaMetricsReporter {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsDReporter.class);

  private final AtomicBoolean running = new AtomicBoolean(false);
  private StatsDClient statsd;
  private AbstractPollingReporter underlying = null;
  private StatsDReporterConfig statsDReporterConfig;

  @Override
  public String getMBeanName() {
    return "kafka:type=" + getClass().getName();
  }

  public boolean isRunning() {
    return running.get();
  }

  //try to make it compatible with kafka-statsd-metrics2
  @Override
  public synchronized void init(VerifiableProperties props) {
    statsDReporterConfig = new StatsDReporterConfig(props);
    if (statsDReporterConfig.isEnabled()) {
      log.info("Reporter is enabled and starting...");
      startReporter(statsDReporterConfig.getPollingPeriodInSeconds());
    } else {
      log.warn("Reporter is disabled");
    }
  }

  @Override
  public void startReporter(long pollingPeriodInSeconds) {
    if (pollingPeriodInSeconds <= 0) {
      throw new IllegalArgumentException("Polling period must be greater than zero");
    }

    synchronized (running) {
      if (running.get()) {
        log.warn("Reporter is already running");
      } else {
        statsd = createStatsd();
        underlying = new StatsDReporter(Metrics.defaultRegistry(), statsd, statsDReporterConfig);
        underlying.start(pollingPeriodInSeconds, TimeUnit.SECONDS);
        log.info("Started Reporter with host={}, port={}, polling_period_secs={}, prefix={}",
            statsDReporterConfig.getHost(), statsDReporterConfig.getPort(), pollingPeriodInSeconds, statsDReporterConfig.getPrefix());
        running.set(true);
      }
    }
  }

  private StatsDClient createStatsd() {
    try {
      return new NonBlockingStatsDClient(
          statsDReporterConfig.getPrefix(),
          statsDReporterConfig.getHost(),
          statsDReporterConfig.getPort()
      );
    } catch (StatsDClientException ex) {
      log.error("Reporter cannot be started");
      throw ex;
    }
  }

  @Override
  public void stopReporter() {
    if (!statsDReporterConfig.isEnabled()) {
      log.warn("Reporter is disabled");
    } else {
      synchronized (running) {
        if (running.get()) {
          underlying.shutdown();
          statsd.stop();
          running.set(false);
          log.info("Stopped Reporter with host={}, port={}", statsDReporterConfig.getHost(), statsDReporterConfig.getPort());
        } else {
          log.warn("Reporter is not running");
        }
      }
    }
  }

}
