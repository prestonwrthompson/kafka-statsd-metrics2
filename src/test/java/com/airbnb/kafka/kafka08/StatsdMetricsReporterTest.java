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

import com.airbnb.metrics.StatsDReporterConfig;
import kafka.utils.VerifiableProperties;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class StatsdMetricsReporterTest {

  private VerifiableProperties properties;

  @Before
  public void init() {
    properties = createMock(VerifiableProperties.class);
    expect(properties.props()).andReturn(new Properties());
    expect(properties.getInt(StatsDReporterConfig.CONFIG_POLLING_INTERVAL_SECS, StatsDReporterConfig.DEFAULT_POLLING_PERIOD_IN_SECONDS)).andReturn(11);
    expect(properties.getString(StatsDReporterConfig.CONFIG_STATSD_HOST, StatsDReporterConfig.DEFAULT_STATSD_HOST)).andReturn("127.0.0.1");
    expect(properties.getInt(StatsDReporterConfig.CONFIG_STATSD_PORT, StatsDReporterConfig.DEFAULT_STATSD_PORT)).andReturn(1234);
    expect(properties.getString(StatsDReporterConfig.CONFIG_STATSD_METRICS_PREFIX, StatsDReporterConfig.DEFAULT_STATSD_PREFIX)).andReturn("foo");
    expect(properties.getString(StatsDReporterConfig.CONFIG_STATSD_EXCLUDE_REGEX,
      StatsDReporterConfig.DEFAULT_STATSD_EXCLUDE_REGEX)).andReturn("foo");
    expect(properties.getString(StatsDReporterConfig.CONFIG_STATSD_INCLUDE_REGEX,
      StatsDReporterConfig.DEFAULT_STATSD_INCLUDE_REGEX)).andReturn("bar");
    expect(properties.getBoolean(StatsDReporterConfig.CONFIG_STATSD_TAG_ENABLED, StatsDReporterConfig.DEFAULT_STATSD_TAG_ENABLED)).andReturn(false);
  }

  @Test
  public void mbean_name_should_match() {
    String name = new StatsdMetricsReporter().getMBeanName();
    assertEquals("kafka:type=com.airbnb.kafka.kafka08.StatsdMetricsReporter", name);
  }

  @Test
  public void init_should_start_reporter_when_enabled() {
    expect(properties.getBoolean("external.kafka.statsd.reporter.enabled", false)).andReturn(true);

    replay(properties);
    StatsdMetricsReporter reporter = new StatsdMetricsReporter();
    assertFalse("reporter should not be running", reporter.isRunning());
    reporter.init(properties);
    assertTrue("reporter should be running once #init has been invoked", reporter.isRunning());

    verify(properties);
  }

  @Test
  public void init_should_not_start_reporter_when_disabled() {
    expect(properties.getBoolean("external.kafka.statsd.reporter.enabled", false)).andReturn(false);

    replay(properties);
    StatsdMetricsReporter reporter = new StatsdMetricsReporter();
    assertFalse("reporter should not be running", reporter.isRunning());
    reporter.init(properties);
    assertFalse("reporter should NOT be running once #init has been invoked", reporter.isRunning());

    verify(properties);
  }
}
