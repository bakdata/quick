# Multi-subscriptions

In [Subscriptions](subscriptions.md), you learned
how to get real-time updates using the `Subscription` type.
The example discussed receiving updates from a single topic.

Quick also allows you to create a so-called multi-subscription.
A multi-subscription enables you to
retrieve complex objects where elements
come from more than one Kafka topic.
To integrate a multi-subscription into your application,
we take the following steps:

1. Modify your schema with and define the multi-subscription.
2. Apply the new schema.
3. Run the multi-subscription.
4. Ingest data.

---

For the purpose of this tutorial,
consider the following scenario:
You want live statistics
of users' clicks and purchases.
Information about clicks and purchases is stored
in separate Kafka topics.
To retrieve such combined users' statistics,
you create a multi-subscription.

## Modify your schema with and define the multi-subscription

As a first step,
add the following to your schema `example.graphql`:
```graphql
type Click {
    userId: Int!
    timestamp: Int
}
```

Next, create a topic that holds `Click` entries:
```shell
quick topic create click --key-type string
 --value-type schema --schema example.Click
```

We can now extend the schema
(from earlier sections of this documentation) as follows:
```graphql
type Subscription {
    userStatistics: UserStatistics
}

type UserStatistics {
    purchases: Purchase @topic(name: "purchase")
    clicks: Click @topic(name: "click")
}
```

Note that multi-subscriptions need different modelling than single subscriptions.
In a [single subscription](subscriptions.md), you add a topic directive
(which references a Kafka topic) to the field that describes the entities
you want to receive updates for, i.e.:
```graphql title="schema.gql"
type Subscription {
  purchases: Purchase @topic(name: "purchase")
}
```

In a multi-subscription, the field `userStatistics` in `Subscription`
is defined through another type, i.e., `UserStatistics`.
This `UserStatistics` comprises the desired fields, each followed by the `@topic` directive.

## Apply the new schema

You can now apply the modified schema to your gateway:  
`quick gateway apply example -f schema.graphql`.

## Run the multi-subscription

To demo the multi-subscription,
we use a GraphQL client, e.g., [Altair](https://altair.sirmuel.design/).
The setup is described [here](subscriptions.md).

Subscribe to user statistics with the following query:
```graphql title="subscription.gql"
subscription {
  userStatistics {
    purchase {
      purchaseId
    }
    click {
      userId
      timestamp
    }
  }
}
```

In Altair, you would press the `Run subscription` button
to start the subscription.

## Ingest data

You can now ingest data to the different topics
and see how the multi-subscription behaves.

Start with adding a single purchase:
```json title="subscription-purchase.json"
{
    "key": "abc",
    "value": {
      "purchaseId": "abc",
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

As a result, you see the following JSON response in your GraphQL client:
```json
{
  "data": {
    "userStatistics": {
      "purchase": {
        "purchaseId": "abc"
      },
      "click": null
    }
  }
}
```

There hasn't been any click event yet,
so the response contains only purchase data.

Now, send the following click event via the ingest service.
!!! Note
    The key of the click event equals the id of the purchase event.
```json title="click1.json"
{
    "key": "abc",
    "value": {
        "userId": 2,
        "timestamp": 123456
    }
}
```
```shell
 curl --request POST --url "$QUICK_URL/ingest/click" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./click1.json"
```

You should see the following subscription result in your GraphQL client:
```json
{ 
  "data": {
    "userStatistics": {
      "purchase": {
        "purchaseId": "abc"
      },
      "click": {
        "userId": 2,
        "timestamp": 123456
      } 
    }
  }
}
```

Now consider another click with the same id `abc` but with a different timestamp.
This causes a new response with the latest timestamp.
```json title="click2.json"
{
    "key": "abc",
    "value": {
        "userId": 2,
        "timestamp": 234567
    }
}
```

As you can see, when a new event of one type, say `Click`, arrives,
you immediately receive the latest state of the other type, here `Purchase`.

Thus, Quick automatically creates an up-to-date response
with elements of the different types stored in different topics.  

This mechanism can be generalized to multi-subscriptions
that comprise more than two types (topics).
For example, in a type that consists of three elements
a new event causes a fetch of the corresponding other types
to create a response immediately.

Please see the [developer section on multi-subscriptions](https://bakdata.github.io/quick/latest/developer/multi-subscriptions-details/),
where we discuss the subtleties of that process.
