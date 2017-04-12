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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.airbnb.metrics.StatsDReporterWrapper;

import kafka.utils.VerifiableProperties;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.slf4j.LoggerFactory;

public class StatsdMetricsReporter implements MetricsReporter {
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsdMetricsReporter.class);
  private static final String REPORTER_NAME = "kafka-statsd-metrics-0.5";

  private StatsDReporterWrapper wrapper = new StatsDReporterWrapper(REPORTER_NAME);

  @Override
  public void init(List<KafkaMetric> metrics) {
    log.info("Init");
    wrapper.startReporter();
  }

  @Override
  public void metricChange(final KafkaMetric metric) {

  }

  @Override
  public void metricRemoval(KafkaMetric metric) {

  }

  @Override
  public void close() {
    log.info("Closing");
    wrapper.stopReporter();
  }

  @Override
  public void configure(Map<String, ?> configs) {
    log.info("Configuring");

    Properties props = new Properties();
    for (Map.Entry<String, ?> entry : configs.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
    VerifiableProperties verifiableProperties = new VerifiableProperties(props);
    wrapper.configure(verifiableProperties);
  }

  protected boolean isRunning() {
    return wrapper.isRunning();
  }
}
