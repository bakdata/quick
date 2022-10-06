# Multi-subscriptions details

This part of the documentation describes the implementation details
of multi-subscriptions.
For the introduction of multi-subscriptions, see:
[Multi-subscriptions](../user/getting-started/working-with-quick/multi-subscriptions.md).

This part outlines what happens under the hood
when users follow the steps for integrating multi-subscriptions
into their applications.  
As a reminder, these steps are:

1. Modify your schema with and define the multi-subscription.
2. Apply the new schema.
3. Run the multi-subscription.
4. Ingest data.

## Modify your schema with and define the multi-subscription

The modification of the schema has no impact
until it is applied.

## Apply the new schema

When a new schema with a multi-subscription is applied,
a [MultiSubscriptionFetcher](https://github.com/bakdata/quick/blob/master/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/subscription/MultiSubscriptionFetcher.java)
is created.
During this process, 
Quick constructs a separate Kafka Consumer for each field
marked with the `@topic` directive.

## Run the multi-subscription

When a query is executed, 
the created Kafka Consumers poll the corresponding topics for events.
When a new event is emitted,
it is sent via WebSocket to the end user.
However, only a part of the complex object is available.
To get the missing part, the _MultiSubscriptionFetcher_
uses either the REST service of the corresponding mirror
or an internal cache to fetch missing data
and create a complex object immediately.
The use of the specific entity is dependent on the current scenario.
There are three scenarios for building complex objects via multi subscriptions:

1. Purchase event arrives; there were some click events,
   but they have not been seen yet by the _MultiSubscriptionFetcher_
   (for example, because the subscription started after the events were produced).
2. Purchase event arrives, and we have already seen the click event for the corresponding id.
3. Purchase event arrives, and there has been no click event.
   Note that in the above considerations, the first event is `Purchase`.
   Note, however, that the choice is arbitrary.
   The system would behave in the same fashion
   if you switched the order of arrival,
   i.e., `Click` and not `Purchase` first.

## Ingest data

Let's say you first ingest a single `Purchase`
and are missing a `Click`.
_MultiSubscriptionFetcher_ first checks the internal cache to see
whether there is a `Click` that refers to the particular id
(the same the `Purchase` relates to).
If a cache hits, _MultiSubscriptionFetcher_ retrieves the value,
creates a complex object,
and returns it to the user (Scenario 2).
If there is a cache miss,
_MultiSubscriptionFetcher_ uses the REST interface of the corresponding client-mirror
and sends a request for the desired key.
If there were previous click events (Scenario 
1), it retrieves a `Click` data,
creates a complex object
and puts the information in the cache for later use.
If it receives nothing from the REST endpoint,
the result depends on the user's decision concerning null values.
If a user permits the delivery of a null value,
an incomplete object will be returned, i.e.:
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
On the other hand,
if null values are not allowed,
a user will receive a subscription error:
```bash
Error: The field at path '/userStatistics/click' was declared as a non null type,
but the code involved in retrieving data has wrongly returned a null value [...].
```






