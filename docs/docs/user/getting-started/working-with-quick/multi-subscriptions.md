In [Subscriptions](subscriptions.md), you learned
how to get real-time updates of your data
using the `Subscription` type. The provided example
of a subscription involved receiving updates from a
single topic. You might wonder if it is possible
to get real-time updates of objects whose components belong to more
than one topic.  
Consider the following scenario: Every 15 seconds,
you want to get statistics of users' clicks and purchases.
Information about clicks and purchases is stored in two separate Kafka topics.
The semantics for multiple subscriptions is slightly different from cases with only one.
In a single subscription, you add a topic directive (which references a specific Kafka topic)
directly to the field that describes the entities you want to receive updates about.
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



The listing below contains some additional remarks regarding multi-subscriptions:
1. It is possible to 
