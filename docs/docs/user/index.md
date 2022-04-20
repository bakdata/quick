# Quick user guide

## What is Quick?

Quick orchestrates your data as a real-time stream of events.
It helps you to run and maintain your apps on your data streams
and exposes GraphQL APIs connecting the data streams to applications and devices.

![Quick overview](../assets/images/quick-architecture.jpg)

Quick runs in a [Kubernetes](https://kubernetes.io/) cluster and on top of [Apache Kafka](https://kafka.apache.org/)
The manager creates, modifies, and deletes other resources.
You interact with it through the [CLI](getting-started/setup-cli).

First, you can create topics in Apache Kafka that hold all your data.
Quick provides an ingest service with a REST API to get data into your topics.
For processing the data,
the manager lets you deploy [Kafka Streams](https://kafka.apache.org/documentation/streams/) applications in the cluster.
They read data from topics, process it, and write it back to a topic.

When you then want to query the data of Apache Kafka topics, you can use the gateway.
The gateway is a [GraphQL](https://graphql.org/) server.
You can apply a GraphQL schema to it that describes the data of your topic.
With the help of mirrors, the gateway can then efficiently query their content.
Next to querying data, it also supports ingesting data through a GraphQL interface.


## User guide

The user guide is split into three parts:

- [Getting Started](getting-started)
- [Examples](examples)
- [Reference](reference)

If you have never worked with Quick, we suggest jumping into the [getting started section](getting-started).
It gives an overview of how to set up Quick and its CLI and shows the first steps when working with it.
The guide also provides [examples](examples) showcasing more complex use cases of Quick.
The [reference](reference) describes Quick's different parts in depth.
