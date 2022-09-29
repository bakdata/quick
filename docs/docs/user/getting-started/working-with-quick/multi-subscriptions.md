# Multi-subscriptions

In [Subscriptions](subscriptions.md), you learned
how to get real-time updates of your data
using the `Subscription` type. The provided example
of a subscription involved receiving updates from a
single topic. You might wonder if it is feasible
to get real-time updates of objects whose components belong to more
than one topic. Quick gives us such a possibility.
You can use a so-called multi-subscription to retrieve complex objects
whose elements belong to more than one Kafka topic. 
Consider the following scenario: Every 15 seconds,
you want to get statistics of users' clicks and purchases.
Information about clicks and purchases is stored in two separate Kafka topics.
To retrieve the information about users' statistics, 
you create a multi-subscription
The semantics for a multi-subscriptions is slightly different from
a typical (single) subscription.
In a single subscription, you add a topic directive (which references a specific Kafka topic)
directly to the field that describes the entities you want to receive updates about, i.e.:
```graphql title="schema.gql"
type Subscription {
  purchases: Purchase @topic(name: "purchase")
}
```
Take a look at the following example to understand the difference in a declaration of a single and multiple
subscriptions (other type definitions have been skipped for clarity and can be consulted in earlier sections of the
documentation).
```graphql title="schema.gql"
type Subscription {
    userStatistics: UserStatistics
}

type UserStatistics {
    purchases: Purchase @topic(name: "purchase")
    clicks: Click @topic(name: "click")
}
```
As you can see, the field `userStatistics` in `Subscription` is not directly annotated with the `@topic` directive.
Instead, the type the field relates to (`UserStatistics`) consists of two fields, each annotated with `@topic` 
directive.  
You can now apply the schema which contains a multi-subscription to your gateway
(`quick gateway apply example -f schema.graphql`).  
To run the multi-subscription, you can use Altair.
The steps for setting it up are described in [Subscriptions](subscriptions.md).
To subscribe to user statistics events, you can run the following query:
```graphql title="subscription.gql"
subscription {
  userStatistics {
    purchase {
      purchaseId
    }
    click {
      userId
    }
  }
}
```
You can press the `Run subscription` button to start the subscription.

We will now ingest some data to different topics and see how the multi-subscription behaves.
Let's start with adding a single purchase:
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

### Additional remarks

The listing below contains some additional remarks regarding multi-subscriptions:
1. Your multi-subscription can refer to more than two topics.
2. Topic can not be nested
