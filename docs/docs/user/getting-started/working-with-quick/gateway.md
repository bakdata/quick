# Gateway

## Creating gateways

One of the core parts of Quick is the gateway.
As the GraphQL interface, it holds the schema describing your data and lets you query it.

The first step when working with gateways is to create a new one:
```shell
quick gateway create example
```
The `example` parameter defines the name of your gateway.
Since you can have many gateways, you will need the name when applying a new schema or querying data from it.

It will take a couple of seconds before the gateway is ready.
You can run `quick gateway describe example` to check the gateway's status.
It should output:
```
Name: example
Replicas: 1
Tag: {{ quick_version }}
```

## The GraphQL schema

After the gateway finished starting, you can apply your first schema.
This guide uses an e-commerce application as an example.
You can start with a basic schema like this:
```graphql title="schema.gql"
type Query {
    findPurchase(purchaseId: String): Purchase 
}

type Purchase {
    purchaseId: String!
    productId: Int!
    userId: Int!
    amount: Int
    price: Price
}

type Product {
    productId: Int!
    name: String
    description: String
    price: Price
}

type Price {
    total: Float
    currency: String
}
```
The central type is `Purchase`, describing a user buying a product.
It links the `Product` type through its field `productId` and a nested type `Price`.

The `Query` type is special in GraphQL since it's a root type that functions as an entry point.
The schema defines a query field `findPurchase`, that takes a `purchaseId` and returns a `Purchase`.
As you may have noticed, some fields have a trailing exclamation mark:
Those fields aren't nullable.

!!! Attention
    Every GraphQL schema requires the `Query` type.
    The gateway won't parse the schema if you don't specify it.
    In case you don't need or want one, you can use an empty query: `type Query {}`.


## Applying a schema

With the GraphQL schema being ready, you can apply it to the created gateway:
```shell
quick gateway apply example -f schema.gql
```
The `example` parameter is the name of the newly created gateway that you can reference like this.
The `-f` flag lets you pass the GraphQL schema to the command.

## Accessing the gateway

You can connect to the GraphQL API of the gateway with the address `$QUICK_URL/gateway/example/graphql`,
where `example` is the name of the gateway you created earlier.
The gateway requires the `QUICK_API_KEY` to be set in the header `X-API-Key`.

You can now use [gql](index.md#prerequisites) to see whether the gateway has the correct schema:
```shell
gql-cli "$QUICK_URL/gateway/example/graphql" \
  -H "X-API-Key:$QUICK_API_KEY" \
  --print-schema
```
Among some other types, the schema as defined earlier should be visible.
