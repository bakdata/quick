# Subscriptions

In [Query data](query-data.md) you have learned to use the `@topic` directive to query data.
This section explains how to get real-time updates of your data.

## Extending the schema

Like the `Query` type, the `Subscription` type works as an entry point for GraphQL.
To create a new subscription, you follow the same approach as with adding a new query:
You add a field to the corresponding type.
In the [example schema](gateway.md#the-graphql-schema), this can look like this:
```graphql title="schema.gql"
type Query {
    findPurchase(purchaseId: String): Purchase @topic(name: "purchase", keyArgument: "purchaseId")
    allPurchases: [Purchase!] @topic(name: "purchase")
}

type Subscription {
  purchases: Purchase @topic(name: "purchase")
}

type Purchase {
    purchaseId: String!
    productId: Int!
    userId: Int!
    product: Product @topic(name: "product", keyField: "productId")
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

Don't forget to re-apply the schema to update Quick:
```shell
quick gateway apply example -f schema.gql
```

Compared to the [latest version of the schema](query-data.md#query-connected-topics),
this schema now has a new type, `Subscription`.
The field `purchases` creates a new subscription.
Whenever Quick receives a new element in the `purchase` topic, it emits an event.

Note, however, that compared to the query fields, the field neither takes a key argument nor returns a list of purchases.
This is because the stream contains purchases with any key, but only one at a time.
It's also possible to include a key argument in a subscription.
Quick then filters the stream, and you only receive updates for the given key.


## Altair Setup

The `gql-cli` doesn't support authentication for subscriptions.
We, therefore, recommend using [Altair](https://altair.sirmuel.design/).
After you have installed it, you need to set it up.
Note that Altair doesn't have access to the variables; therefore, you must replace them with the appropriate values.

1. In the field `Enter URL`, enter `$QUICK_URL/gateway/example/graphql`
1. In the left menu, click on "Set Headers" and add a new header with key `X-API-Key` and the value of `$QUICK_API_KEY`
1. In the left menu, click on "Subscription URL"
    1. Set the URL to `ws://$QUICK_HOST/gateway/example/graphql-ws`.
       Note that the URL uses `ws` as protocol and has `-ws` suffix.
    1. Set the connection parameters (where `$QUICK_API_KEY` is your key)
      ```json
        {
        "X-API-Key": "$QUICK_API_KEY"
        }
      ```

## Running the subscription
To subscribe to the purchase events, you can run the following query in Altair:
```graphql title="subscription.gql"
subscription {
  purchases {
    product {
      name
      description
    }
    amount
    price {
      total
      currency
    }
  }
}
```

You can press the `Run subscription` button to start the subscription.

---
You can now ingest new data into the `purchase` topic and will receive them in your subscription.
You can test this by using the [purchases.json from the ingest data section](ingest-data.md).
For example, ingest the following purchase:
```json title="subscription-purchase.json"
{
    "key": "aj",
    "value": {
      "purchaseId": "aj",
      "productId": 123,
      "userId": 2,
      "amount": 2,
      "price": {
        "total": 29.99,
        "currency": "DOLLAR"
      }
    }
}
```

```shell
 curl --request POST --url "$QUICK_URL/ingest/purchase" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./subscription-purchase.json"
```

The subscription should emit the following event:
```json
 {
  "data": {
    "purchases": {
      "product": {
        "name": "T-Shirt",
        "description": "black"
      },
      "amount": 2,
      "price": {
        "total": 29.99,
        "currency": "DOLLAR"
      }
    }
  }
} 
```
Again, you can see that Quick automatically extends the data with the product information.
