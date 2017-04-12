package com.airbnb.metrics;

import kafka.utils.VerifiableProperties;

import java.util.EnumSet;

/**
 * Config class to parse Properties for those applicable to the StatsDReporter
 */
public class StatsDReporterConfig {
  public static final String DEFAULT_STATSD_EXCLUDE_REGEX = "(kafka\\.server\\.FetcherStats.*ConsumerFetcherThread.*)|(kafka\\.consumer\\.FetchRequestAndResponseMetrics.*)|(.*ReplicaFetcherThread.*)|(kafka\\.server\\.FetcherLagMetrics\\..*)|(kafka\\.log\\.Log\\..*)|(kafka\\.cluster\\.Partition\\..*)";
  public static final String DEFAULT_STATSD_INCLUDE_REGEX = null;
  public static final int DEFAULT_POLLING_PERIOD_IN_SECONDS = 10;
  public static final String DEFAULT_STATSD_HOST = "localhost";
  public static final int DEFAULT_STATSD_PORT = 8125;
  public static final String DEFAULT_STATSD_PREFIX = "";
  public static final boolean DEFAULT_STATSD_TAG_ENABLED = true;

  public static final String CONFIG_STATSD_REPORTER_ENABLED = "external.kafka.statsd.reporter.enabled";
  public static final String CONFIG_STATSD_HOST = "external.kafka.statsd.host";
  public static final String CONFIG_STATSD_PORT = "external.kafka.statsd.port";
  public static final String CONFIG_STATSD_METRICS_PREFIX = "external.kafka.statsd.metrics.prefix";
  public static final String CONFIG_POLLING_INTERVAL_SECS = "kafka.metrics.polling.interval.secs";
  public static final String CONFIG_STATSD_DIMENSION_ENABLED = "external.kafka.statsd.dimension.enabled";
  public static final String CONFIG_STATSD_EXCLUDE_REGEX = "external.kafka.statsd.metrics.exclude_regex";
  public static final String CONFIG_STATSD_INCLUDE_REGEX = "external.kafka.statsd.metrics.include_regex";
  public static final String CONFIG_STATSD_TAG_ENABLED = "external.kafka.statsd.tag.enabled";

  private boolean enabled;
  private String host;
  private int port;
  private String prefix;
  private long pollingPeriodInSeconds;
  private EnumSet<Dimension> metricDimensions;
  private SharedMetricPredicate metricPredicate;
  private boolean isTagEnabled;

  public StatsDReporterConfig(VerifiableProperties properties) {
    enabled = properties.getBoolean(CONFIG_STATSD_REPORTER_ENABLED, false);
    host = properties.getString(CONFIG_STATSD_HOST, DEFAULT_STATSD_HOST);
    port = properties.getInt(CONFIG_STATSD_PORT, DEFAULT_STATSD_PORT);
    prefix = properties.getString(CONFIG_STATSD_METRICS_PREFIX, DEFAULT_STATSD_PREFIX);
    pollingPeriodInSeconds = properties.getInt(CONFIG_POLLING_INTERVAL_SECS, DEFAULT_POLLING_PERIOD_IN_SECONDS);
    metricDimensions = Dimension.fromProperties(properties.props(), CONFIG_STATSD_DIMENSION_ENABLED + ".");

    SharedMetricPredicate excludeMetricPredicate, includeMetricPredicate;
    String excludeRegex = properties.getString(CONFIG_STATSD_EXCLUDE_REGEX, DEFAULT_STATSD_EXCLUDE_REGEX);
    if (excludeRegex != null && excludeRegex.length() != 0) {
      excludeMetricPredicate = new ExcludeMetricPredicate(excludeRegex);
    } else {
      excludeMetricPredicate = SharedMetricPredicate.ALL;
    }

    String includeRegex = properties.getString(CONFIG_STATSD_INCLUDE_REGEX, DEFAULT_STATSD_INCLUDE_REGEX);
    if (includeRegex != null && includeRegex.length() != 0) {
      includeMetricPredicate = new IncludeMetricPredicate(includeRegex);
    } else {
      includeMetricPredicate = SharedMetricPredicate.ALL;
    }
    metricPredicate = new ConjunctionMetricPredicate(excludeMetricPredicate, includeMetricPredicate);

    isTagEnabled = properties.getBoolean(CONFIG_STATSD_TAG_ENABLED, DEFAULT_STATSD_TAG_ENABLED);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isTagEnabled() {
    return isTagEnabled;
  }

  public EnumSet<Dimension> getMetricDimensions() {
    return metricDimensions;
  }

  public int getPort() {
    return port;
  }

  public String getHost() {
    return host;
  }

  public String getPrefix() {
    return prefix;
  }

  public long getPollingPeriodInSeconds() {
    return pollingPeriodInSeconds;
  }

  public SharedMetricPredicate getMetricPredicate() {
    return metricPredicate;
  }
}