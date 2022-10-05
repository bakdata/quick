# Multi-subscriptions

In [Subscriptions](subscriptions.md), you learned
how to get real-time data updates
using the `Subscription` type.
The provided example of a subscription involved
receiving updates from a single topic.
However, Quick also gives you the possibility
to create a so-called multi-subscription.
A multi-subscription enables you to
retrieve complex objects whose elements
belong to more than one Kafka topic.
To integrate a multi-subscription into your application,
you make the following steps:
1. Modify your schema with and define the multi-subscription.
2. Apply the new schema.
3. Run the multi-subscription.
4. Ingest data.

---

For the purpose of this tutorial,
consider the following scenario:
Every 15 seconds, you want to get statistics
of users' clicks and purchases.
Information about clicks and purchases is stored
in two separate Kafka topics.
To retrieve the information about users' statistics,
you create a multi-subscription.

## Modify your schema with and define the multi-subscription

Before the details of multi-subscriptions are presented,
please extend your schema `example.graphql` with the following type:
```graphql
type Click {
    userId: Int!
    timestamp: Int
}
```
Additionally, please create a topic that holds entries of the `Click` type:
```shell
quick topic create click --key-type string
 --value-type schema --schema example.Click
```
To introduce multi-subscriptions,
we will extend the schema
(please consult the earlier sections of the documentation
if you don't have the previous schema) as follows:
```graphql
type Subscription {
    userStatistics: UserStatistics
}

type UserStatistics {
    purchases: Purchase @topic(name: "purchase")
    clicks: Click @topic(name: "click")
}
```
As you can see, the semantics for multi-subscriptions is slightly
different from a typical (single) subscription.  
As a reminder, in a [single subscription](subscriptions.md),
you add a topic directive
(which references a specific Kafka topic)
directly to the field that describes the entities
you want to receive updates about, i.e.:
```graphql title="schema.gql"
type Subscription {
  purchases: Purchase @topic(name: "purchase")
}
```
In a multi-subscription, the field `userStatistics` in `Subscription`
is **not** directly applied with the `@topic` directive.
Instead, the type the field relates to (`UserStatistics`)
consists of two fields, each with the `@topic`directive.

## Apply the new schema

You can now apply the schema containing a multi-subscription to your gateway:
(`quick gateway apply example -f schema.graphql`).

## Run the multi-subscription

To run the multi-subscription,
it is recommended to use a GraphQL client
(for example, [Altair](https://altair.sirmuel.design/)).
The steps for setting it up are described in [Subscriptions](subscriptions.md).
To subscribe to user statistics events,
you can run the following query:
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

You can now ingest some data to different topics
and see how the multi-subscription behaves.
Please start with adding a single purchase:
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
As a result, you should see the following JSON response in Altair:
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
Since you have not added any click event,
the response contains only purchase data.
Now, please send the following click event
via the ingest service.
!!! Note
Please note that the key of the click event
is the same as the id of the purchase event.
Otherwise, they couldn't be associated.
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
{ "data": {
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
```json title="click1.json"
{
    "key": "abc",
    "value": {
        "userId": 2,
        "timestamp": 234567
    }
}
```
If you add yet another click with the same id as before
but with a different timestamp and ingest it,
you will get almost the same response as before.
The only thing that changes is the field timestamp.
As you can see, when a new event of a specific type arrives
(a `Click` event in the example above),
you immediately receive the latest seen version of the other type
(a `Purchase` event in the example).
Quick does the automatic retrieval to directly
create a response whose elements
belong to different types 
and are stored in various topics.
This mechanism can be extrapolated to multi-subscriptions
that encompass multiple types (topics).
If you had a type that consists of three elements,
and you receive the first element,
the latest versions of the corresponding types are fetched
immediately to create a response.
If you are interested in the details of this process,
you are welcome to consult
the developer [section on multi-subscriptions](https://bakdata.github.io/quick/latest/developer/multi-subscriptions-details/)
where the intricacies of the process are explained.
