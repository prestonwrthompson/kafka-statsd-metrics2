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

import com.airbnb.metrics.StatsDReporterWrapper;

import kafka.metrics.KafkaMetricsReporter;
import kafka.utils.VerifiableProperties;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class StatsdMetricsReporter implements StatsdMetricsReporterMBean, KafkaMetricsReporter {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(StatsdMetricsReporter.class);
  private static final String REPORTER_NAME = "kafka-statsd-metrics";

  private StatsDReporterWrapper wrapper = new StatsDReporterWrapper(REPORTER_NAME);

  @Override
  public String getMBeanName() {
    return "kafka:type=" + getClass().getName();
  }

  protected boolean isRunning() {
    return wrapper.isRunning();
  }

  //try to make it compatible with kafka-statsd-metrics2
  @Override
  public synchronized void init(VerifiableProperties props) {
    wrapper.configure(props);
    wrapper.startReporter();
  }

  @Override
  public void startReporter(long pollingPeriodInSeconds) {
    wrapper.startReporter(pollingPeriodInSeconds);
  }

  @Override
  public void stopReporter() {
    wrapper.stopReporter();
  }

}
