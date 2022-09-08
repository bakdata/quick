# Working with Quick

With Quick and Quick CLI installed, you can start working with Quick.
This guide looks into different aspects when working with Quick by following an example use case of an e-commerce application:

- The [Gateway](gateway.md) is one of Quick's core modules.
  You start by creating a new gateway, applying a schema, and connecting to the gateway.
- [Topics](topics.md) store events in Quick and let you share data between applications.
  The guide gives an overview of how you can create and delete topics.
- The [ingest service](ingest-data.md) lets you get data into the platform through a REST API.
- In [Querying data](query-data.md), you connect your gateway and topics.
  This lets you query the data of topics and create relationships between them.
- In [Range Queries](range-query.md), you can learn how to use Range Queries, which is a new feature available
  from version 0.8 that enables you to retrieve values from a given topic according to a specific range of a particular field.
- The guide closes this section with [subscriptions](subscriptions.md).
  They let you receive real-time updates from the gateway.


## Prerequisites

- A tool for querying GraphQL endpoints.
  This guide uses [gql](https://github.com/graphql-python/gql).
  You can install it with pip:
  ```shell
  pip install 'gql[all]==3.1.0'
  ```
- (Optional) [jq](https://stedolan.github.io/jq/) to pretty-print JSON output.
  There are many options for installing jq. 
  Please see [their download page](https://stedolan.github.io/jq/download/).
