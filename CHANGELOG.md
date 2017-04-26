# 0.5.3

- Adds support for `exclude_regex` configuration option in kafka09
- Adds support for `include_regex` configuration option for both kafka08 and kafka09
- When tags are disabled (i.e. `external.kafka.statsd.tag.enabled` is set to false) for kafka09, the metric name sent to statsd will now include the tag keys and values separated by periods. For example, a metric with group `group`, name `name`, and tags `key1:value1` and `key2:value2` will be sent to statsd as `kafka.group.key1.value1.key2.value2.name`
- Refactors configuration settings. The only configuration options not followed by the kafka09 reporter at this point are the dimension configurations.

# 0.5.2

- Convert INFINITY values to 0.

# 0.4.0

 - `0.4.0` adds support for tags on metrics. See [dogstatsd extensions](http://docs.datadoghq.com/guides/dogstatsd/#tags). If your statsd server does not support tags, you can disable them in the Kafka configuration. See property `external.kafka.statsd.tag.enabled` below.

 - The statsd client is [`com.indeed:java-dogstatsd-client:2.0.11`](https://github.com/indeedeng/java-dogstatsd-client/tree/java-dogstatsd-client-2.0.11).
 - support new `MetricNames` introduced by kafka `0.8.2.x`
 - remove JVM metrics. Only the metrics from Kafka `MetricRegistry` are sent.

# 0.3.0
- send JVM metrics

