# Topics

With the initial schema applied to the gateway, you can create new topics.
Because they're responsible for storing all data in Apache Kafka, topics are a fundamental part of Quick.

## Creating new topics

Recalling [the schema](gateway.md#the-graphql-schema) from the previous section,
there are two types of data: `Purchase` and `Product`.
The application should be able to store the data of both types.
Therefore, the next step is to create the corresponding topics.

First, you create the topic for `Purchase` by running:
```shell
quick topic create purchase \
  --key-type string --value-type schema --schema example.Purchase 
```

The first parameter is `purchase`, the topic's name you create.

The `key-type` and `value-type` options define the key and value types, respectively.
Quick ensures that data ingested into this topic conforms to these types.
If you'd try to ingest data with a number as the key into the `purchase` topic, Quick throws an error.

Next to primitives types like `string`, Quick also supports complex types defined by `schema`.
When using the schema option, you have to tell Quick what this schema should look like.
You can do this by referencing a type of GraphQL schema applied to a gateway.
In this case, you want first to reference the gateway, here called `example`.
In `example`, the `Purchase` type corresponds to the data in this topic.
Therefore, the `--schema` option is `example.Purchase`.

You can now also create the product topic and a price topic:
```shell
quick topic create product --key-type long --value-type schema --schema example.Product && 
  quick topic create price --key-type long --value-type schema --schema example.Price
```

In contrast to the `purchase` topic, this topic has a key type `long`.

## Topic information

The Quick CLI comes with commands to view the current state of topics in Quick.
First, you can take a look at existing topics:
```shell
quick topic list
```
Next, you can also view more detailed information about a topic:
```shell
quick topic describe purchase
```
This command returns the types and schema of the topic.

## Deleting topics

You can also delete topics.
For example, the price topic is no longer needed, and you want to remove it:
```shell
quick topic delete price
```



