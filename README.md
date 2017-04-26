[![Build Status](https://travis-ci.org/airbnb/kafka-statsd-metrics2.png?branch=master)](https://travis-ci.org/airbnb/kafka-statsd-metrics2)

# kafka-statsd-metrics2

Send Kafka Metrics to StatsD.

## Contact
**Let us know!** If you fork this, or if you use it, or if it helps in anyway, we'd love to hear from you! opensource@airbnb.com

## What is it about?
Kafka reports metrics about the state of its brokers, clients, Streams, and Connect. These various components can be configured to report stats using pluggable stats reporters that can be hooked up to your monitoring systems.

This project provides a simple integration between Kafka and a StatsD reporter for Metrics.

Metrics can be filtered based on the metric name and the metric dimensions (min, max, percentiles, etc).

### A Brief History of Metrics Reporting in Kafka

Kafka has a strange and poorly documented history of its metrics reporting.
At first, Kafka started off using [Yammer Metrics](http://metrics.codahale.com/getting-started/) (now part of the [Dropwizard project](http://metrics.codahale.com/about/)) for [metrics reporting](https://kafka.apache.org/documentation.html#monitoring) for the broker and client.
There were some compatibility problems with the Yammer Metrics library, so the minds behind Kafka decided to create their own metrics reporting library for Kafka that did essentially the same thing as Yammer Metrics.
You can read the initial proposal for rewriting the metrics library [here](http://markmail.org/message/5t2xkr57vivjd65u). They wrote the new clients for Kafka 0.9+ to use only this new native metrics reporting and not Yammer Metrics.
However, they did not update all the existing broker metrics to use this new native reporting. Instead, they added new broker metrics using the new native reporting and left the existing broker metrics to use Yammer Metrics.
To make matters worse, they continue to add new broker metrics that use the Yammer Metrics library. Yikes! There is a [JIRA issue](https://issues.apache.org/jira/browse/KAFKA-1930) to move all broker metrics from Yammer Metrics to the native metrics reporting.

Some other relevant JIRA issues are [here](https://issues.apache.org/jira/browse/KAFKA-960), [here](https://issues.apache.org/jira/browse/KAFKA-826), and [here](https://issues.apache.org/jira/browse/KAFKA-542).

### Why is this important?

[Kafka's documentation](http://kafka.apache.org/documentation/#configuration) lists configuration options for the various components.
In these options, you can find the pluggable stats reporter option of `metric.reporters` available for the broker, new consumer, producer, Connect, and Streams.
This option accepts a comma-separated list of class names that implement `org.apache.kafka.common.metrics.MetricsReporter`, each of which will be instantiated on startup and when metrics are created or removed the reporters will be notified.
The problem is that these metric reporters are only ever notified of metrics that use the new native reporting.

To access metrics that use Yammer Metrics (which includes a majority of the broker metrics), there is another configuration option `kafka.metrics.reporters` that is no longer listed in the documentation! A [JIRA issue](https://issues.apache.org/jira/browse/KAFKA-5066) was recently added to re-expose this configuration option.
This option accepts a comma-separated list of class names that implement `kafka.metrics.KafkaMetricsReporter`, each of which will simply be started on startup and stopped on shutdown.

This project has implemented both types of reporters. `com.airbnb.kafka.kafka08.StatsdMetricsReporter` implements `kafka.metrics.KafkaMetricsReporter` and can be used in the `kafka.metrics.reporters` configuration option to get access to metrics reported via the Yammer Metrics.
`com.airbnb.kafka.kafka09.StatsdMetricsReporter` implements `org.apache.kafka.common.metrics.MetricsReporter` and can be used in the `metric.reporters` configuration option to get access the metrics reported via the native metric reporting.

The following table is a guide to which you should use:

| Component | 0.8        | 0.9     | 0.10    |
|-----------|------------|---------|---------|
| broker    | kafka08    | both    | both    |
| consumer  | no metrics | kafka09 | kafka09 |
| producer  | no metrics | kafka09 | kafka09 |
| Connect   | N/A        | N/A     | kafka09 |
| Streams   | N/A        | N/A     | kafka09 |

## Supported Kafka versions

- For Kafka `0.9.0.0` or later use `kafka-statsd-metrics2-0.5.0`
- For Kafka `0.8.2.0` or later use `kafka-statsd-metrics2-0.4.0`
- For Kafka `0.8.1.1` or prior use `kafka-statsd-metrics2-0.3.0`


## Releases

### 0.5.3

- Adds support for `exclude_regex` configuration option in kafka09
- Adds support for `include_regex` configuration option for both kafka08 and kafka09
- When tags are disabled (i.e. `external.kafka.statsd.tag.enabled` is set to false) for kafka09, the metric name sent to statsd will now include the tag keys and values separated by periods. For example, a metric with group `group`, name `name`, and tags `key1:value1` and `key2:value2` will be sent to statsd as `kafka.group.key1.value1.key2.value2.name`
- Refactors configuration settings. The only configuration options not followed by the kafka09 reporter at this point are the dimension configurations.

### 0.5.2
- Convert INFINITY values to 0.

### 0.5.1
- Fix metrics change log level

### 0.5.0

 - `0.5.0` add support to report new producer/consumer metrics in kafka-0.9
 - Compatible with Kafka 0.8
 - A complete list of all the metrics supported in the metrics reporter can be found [here](http://docs.confluent.io/2.0.1/kafka/monitoring.html)


### 0.4.0

 - `0.4.0` adds support for tags on metrics. See [dogstatsd extensions](http://docs.datadoghq.com/guides/dogstatsd/#tags). If your statsd server does not support tags, you can disable them in the Kafka configuration. See property `external.kafka.statsd.tag.enabled` below.

 - The statsd client is [`com.indeed:java-dogstatsd-client:2.0.11`](https://github.com/indeedeng/java-dogstatsd-client/tree/java-dogstatsd-client-2.0.11).
 - support new `MetricNames` introduced by kafka 0.8.2.x

### 0.3.0

- initial release

## How to install?

- [Download](https://bintray.com/airbnb/jars/kafka-statsd-metrics2/view) or build the shadow jar for `kafka-statsd-metrics`.
- Install the jar in Kafka classpath, typically `./kafka_2.11-0.9.0.1/libs/`
- In the Kafka config file, `server.properties`, add the following properties. Default values are in parenthesis.

## How to use metrics in Kafka 0.9 / 0.8?
### New metrics in kafka 0.9

1. Add `metric.reporters` in producer.properties or consumer.properties
```bash
    # declare the reporter if new producer/consumer is used
    metric.reporters=com.airbnb.kafka.kafka09.StatsdMetricsReporter
```
2. Run new-producer or new-consumer

Producer:
```bash
    bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test --producer.config config/producer.properties
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --new-consumer --zookeeper localhost:2181 --topic test -from-beginning --consumer.config config/consumer.properties    
```

### Old metrics in kafka 0.8

1. Add `kafka.metrics.reporters` in producer.properties or consumer.properties
```bash
    # declare the reporter if old producer/consumer is used
    kafka.metrics.reporters=com.airbnb.kafka.kafka08.StatsdMetricsReporter
```
2. Run old-producer or old-consumer

```bash
    bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test --producer.config config/producer.properties --old-producer
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --zookeeper localhost:2181 --topic test -from-beginning --consumer.config config/consumer.properties
```

2. Run old-consumer


### Configurations
```bash
    # declare the reporter if new producer/consumer is used
    metric.reporters=com.airbnb.kafka.StatsdMetricsReporter

    # declare the reporter if old producer/consumer is used
    kafka.metrics.reporters=com.airbnb.kafka.kafka08.StatsdMetricsReporter

    # enable the reporter, (false)
    external.kafka.statsd.reporter.enabled=true

    # the host of the StatsD server (localhost)
    external.kafka.statsd.host=localhost

    # the port of the StatsD server (8125)
    external.kafka.statsd.port=8125

    # enable the support of statsd tag extension, e.g. datadog statsd (true)
    external.kafka.statsd.tag.enabled=true

    # a prefix for all metrics names (empty)
    external.kafka.statsd.metrics.prefix=

    # note that the StatsD reporter follows the global polling interval (10)
    # kafka.metrics.polling.interval.secs=10



    # A regex to exclude some metrics
    # Default is: (kafka\.consumer\.FetchRequestAndResponseMetrics.*)|(.*ReplicaFetcherThread.*)|(kafka\.server\.FetcherLagMetrics\..*)|(kafka\.log\.Log\..*)|(kafka\.cluster\.Partition\..*)
    #
    # The metric name is formatted with this template: group.type.scope.name
    #
    # external.kafka.statsd.metrics.exclude_regex=


    # Each metric provides multiple dimensions: min, max, meanRate, etc
    # This might be too much data.
    # It is possible to disable some metric dimensions with the following properties:
    # By default all dimenstions are enabled.
    # Does not apply to kafka09
    #
    # external.kafka.statsd.dimension.enabled.count=true
    # external.kafka.statsd.dimension.enabled.meanRate=true
    # external.kafka.statsd.dimension.enabled.rate1m=true
    # external.kafka.statsd.dimension.enabled.rate5m=true
    # external.kafka.statsd.dimension.enabled.rate15m=true
    # external.kafka.statsd.dimension.enabled.min=true
    # external.kafka.statsd.dimension.enabled.max=true
    # external.kafka.statsd.dimension.enabled.mean=true
    # external.kafka.statsd.dimension.enabled.stddev=true
    # external.kafka.statsd.dimension.enabled.median=true
    # external.kafka.statsd.dimension.enabled.p75=true
    # external.kafka.statsd.dimension.enabled.p95=true
    # external.kafka.statsd.dimension.enabled.p98=true
    # external.kafka.statsd.dimension.enabled.p99=true
    # external.kafka.statsd.dimension.enabled.p999=true
```

- finally restart the Kafka server

## How to test your configuration?

You can check your configuration in different ways:

- During Kafka startup, the reporter class will be instantiated and initialized. The logs should contain a message similar to:
`"Kafka Statsd metrics reporter is enabled"`
- A JMX MBean named `kafka:type=com.airbnb.kafka.kafka08.StatsdMetricsReporter` should also exist.
- Check the logs of your StatsD server
- Finally, on the configured StatsD host, you could listen on the configured port and check for incoming data:

```bash
    # assuming the Statsd server has been stopped...
    $ nc -ul 8125

    kafka.controller.ControllerStats.LeaderElectionRateAndTimeMs.samples:1|gkafka.controller.ControllerStats
    .LeaderElectionRateAndTimeMs.meanRate:0.05|gkafka.controller.ControllerStats.LeaderElectionRateAndTimeMs.
    1MinuteRate:0.17|gkafka.controller.ControllerStats.LeaderElectionRateAndTimeMs.5MinuteRate:0.19|g....
```

## Sample file of metrics output from statsd
[new-producer-metrics.txt](https://www.dropbox.com/s/p8e4vl5moa80ikp/new-producer-metrics.txt?dl=0)

[new-consumer-metrics.txt](https://www.dropbox.com/s/ab3t8qis5p58l7f/new-consumer-metrics.txt?dl=0)


## List of metrics for Kafka 0.8.2

Below are the metrics in Kafka 0.8.2

```bash
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Metrics kind | Metric Name                                                                 | Metric Tags                                                                                                 |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ReplicaManager.LeaderCount                                     |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ReplicaManager.PartitionCount                                  |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ReplicaManager.UnderReplicatedPartitions                       |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.controller.KafkaController.ActiveControllerCount                      |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.controller.KafkaController.OfflinePartitionsCount                     |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.controller.KafkaController.PreferredReplicaImbalanceCount             |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.network.RequestChannel.RequestQueueSize                               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ReplicaFetcherManager.Replica_MaxLag                           | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ReplicaFetcherManager.Replica_MinFetchRate                     | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.FetchRequestPurgatory.PurgatorySize                            |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.FetchRequestPurgatory.NumDelayedRequests                       |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ProducerRequestPurgatory.PurgatorySize                         |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.ProducerRequestPurgatory.NumDelayedRequests                    |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.consumer.ConsumerFetcherManager.MaxLag                                | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.consumer.ConsumerFetcherManager.MinFetchRate                          | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.consumer.ZookeeperConsumerConnector.FetchQueueSize                    | {"clientId" -> config.clientId, "topic" -> topic, "threadId" -> thread}                                     |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.network.RequestChannel.ResponseQueueSize                              | {"Processor" -> i}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.log.LogFlushStats.LogFlushRateAndTimeMs                               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.ReplicaManager.IsrExpandsPerSec                                |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.ReplicaManager.IsrShrinksPerSec                                |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.DelayedFetchRequestMetrics.FollowerExpiresPerSecond            |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.DelayedFetchRequestMetrics.ConsumerExpiresPerSecond            |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.controller.ControllerStats.UncleanLeaderElectionsPerSec               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.controller.ControllerStats.LeaderElectionRateAndTimeMs                |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerStats.SerializationErrorsPerSec                      | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerStats.ResendsPerSec                                  | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerStats.FailedSendsPerSec                              | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.MessagesPerSec_all                      | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.BytesPerSec_all                         | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.DroppedMessagesPerSec_all               | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.MessagesPerSec                          | {"clientId" -> clientId, "topic" -> topic}                                                                  |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.BytesPerSec                             | {"clientId" -> clientId, "topic" -> topic}                                                                  |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.producer.ProducerTopicMetrics.DroppedMessagesPerSec                   | {"clientId" -> clientId, "topic" -> topic}                                                                  |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.FetcherStats.RequestsPerSec                                    | {"clientId" -> metricId.clientId, "brokerHost" -> metricId.brokerHost, "brokerPort" -> metricId.brokerPort} |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.FetcherStats.BytesPerSec                                       | {"clientId" -> metricId.clientId, "brokerHost" -> metricId.brokerHost, "brokerPort" -> metricId.brokerPort} |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.MessagesInPerSec_all                        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.BytesInPerSec_all                           |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.BytesOutPerSec_all                          |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.LogBytesAppendedPerSec_all                  |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.FailedProduceRequestsPerSec_all             |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.FailedFetchRequestsPerSec_all               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.MessagesInPerSec                            | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.BytesInPerSec                               | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.BytesOutPerSec                              | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.LogBytesAppendedPerSec                      | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.FailedProduceRequestsPerSec                 | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.BrokerTopicMetrics.FailedFetchRequestsPerSec                   | {"topic" -> topic}                                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.DelayedProducerRequestMetrics.ExpiresPerSecond_all             |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.DelayedProducerRequestMetrics.ExpiresPerSecond                 | {"topic" -> topicAndPartition.topic, "partition" -> topicAndPartition.partition}                            |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.producer.ProducerRequestMetrics.ProducerRequestRateAndTimeMs_all      | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.producer.ProducerRequestMetrics.ProducerRequestSize_all               | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.consumer.FetchRequestAndResponseMetrics.FetchRequestRateAndTimeMs_all | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.consumer.FetchRequestAndResponseMetrics.FetchResponseSize_all         | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.producer.ProducerRequestMetrics.ProducerRequestRateAndTimeMs          | {"clientId" -> clientId, "brokerHost" -> brokerHost, "brokerPort" -> brokerPort}                            |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.producer.ProducerRequestMetrics.ProducerRequestSize                   | {"clientId" -> clientId, "brokerHost" -> brokerHost, "brokerPort" -> brokerPort}                            |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | kafka.consumer.FetchRequestAndResponseMetrics.FetchRequestRateAndTimeMs     | {"clientId" -> clientId, "brokerHost" -> brokerHost, "brokerPort" -> brokerPort}                            |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.consumer.FetchRequestAndResponseMetrics.FetchResponseSize             | {"clientId" -> clientId, "brokerHost" -> brokerHost, "brokerPort" -> brokerPort}                            |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ConsumerTopicMetrics.MessagesPerSec_all                      | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ConsumerTopicMetrics.BytesPerSec_all                         | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ConsumerTopicMetrics.MessagesPerSec                          | {"clientId" -> clientId, "topic" -> topic}                                                                  |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ConsumerTopicMetrics.BytesPerSec                             | {"clientId" -> clientId, "topic" -> topic}                                                                  |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.network.RequestMetrics.RequestsPerSec                                 | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.RequestQueueTimeMs                             | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.LocalTimeMs                                    | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.RemoteTimeMs                                   | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.ResponseQueueTimeMs                            | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.ResponseSendTimeMs                             | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.network.RequestMetrics.TotalTimeMs                                    | {"request" -> name}                                                                                         |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.FetcherLagMetrics.ConsumerLag                                  | {"clientId" -> clientId, "topic" -> topic, "partition" -> partitionId}                                      |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.producer.async.ProducerSendThread.ProducerQueueSize                   | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.Log.NumLogSegments                                                | {"topic" -> topic, "partition" -> partition}                                                                |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.Log.LogEndOffset                                                  | {"topic" -> topic, "partition" -> partition}                                                                |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.cluster.Partition.UnderReplicated                                     | {"topic" -> topic, "partition" -> partitionId}                                                              |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.network.SocketServer.ResponsesBeingSent                               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.network.SocketServer.NetworkProcessorAvgIdlePercent                   |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.network.SocketServer.IdlePercent                                      | {"networkProcessor" -> i.toString}                                                                          |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.OffsetManager.NumOffsets                                       |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.OffsetManager.NumGroups                                        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.consumer.ZookeeperConsumerConnector.OwnedPartitionsCount              | {"clientId" -> config.clientId, "groupId" -> config.groupId}                                                |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.consumer.ZookeeperConsumerConnector.OwnedPartitionsCount              | {"clientId" -> config.clientId, "groupId" -> config.groupId, "topic" -> topic}                              |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ZookeeperConsumerConnector.KafkaCommitsPerSec                | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ZookeeperConsumerConnector.ZooKeeperCommitsPerSec            | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.consumer.ZookeeperConsumerConnector.RebalanceRateAndTime              | {"clientId" -> clientId}                                                                                    |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.tools.DataChannel.MirrorMaker-DataChannel-WaitOnPut                   |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.tools.DataChannel.MirrorMaker-DataChannel-WaitOnTake                  |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Histogram    | kafka.tools.DataChannel.MirrorMaker-DataChannel-Size                        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.common.AppInfo.Version                                                |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.server.KafkaRequestHandlerPool.RequestHandlerAvgIdlePercent           |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Meter        | kafka.util.Throttler."""a input string not with small cardinality"""        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.LogCleaner.max-buffer-utilization-percent                         |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.LogCleaner.cleaner-recopy-percent                                 |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.LogCleaner.max-clean-time-secs                                    |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | other.kafka.FetchThread.fetch-thread                                        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Timer        | other.kafka.CommitThread.commit-thread                                      |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.Log.LogStartOffset                                                | {"topic" -> topic, "partition" -> partition}                                                                |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.Log.Size                                                          | {"topic" -> topic, "partition" -> partition)                                                                |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.server.KafkaServer.BrokerState                                        |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+
    | Gauge        | kafka.log.LogCleanerManager.max-dirty-percent                               |                                                                                                             |
    +--------------+-----------------------------------------------------------------------------+-------------------------------------------------------------------------------------------------------------+```
```

## Metrics-2.x vs Metrics-3.x
The metrics project has two main versions: v2 and v3. Version 3 is not backward compatible.

Version [0.8.1.1](https://github.com/apache/kafka/blob/0.8.1.1/build.gradle#L217) and [0.8.2.1](https://github.com/apache/kafka/blob/0.8.2.1/build.gradle#L209), Kafka depends on [metrics-2.2.0](http://mvnrepository.com/artifact/com.yammer.metrics/metrics-core/2.2.0).

*Note:*<br/>
In a future release, Kafka [might upgrade](https://issues.apache.org/jira/browse/KAFKA-960) to Metrics-3.x.
Due to the incompatibilities between Metrics versions, a new Statsd reporter for metrics-3 will be required.<br/>
All contributions welcome!


## How to build

After cloning the repo, type

```bash
    ./gradlew shadowJar
```

This produces a jar file in `build/libs/`.

The shallow jar is a standalone jar.


# License & Attributions

This project is released under the Apache License Version 2.0 (APLv2).
