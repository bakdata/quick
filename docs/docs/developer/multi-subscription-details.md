# Multi-subscriptions details

The introduction to multi-subscriptions is
[here](../user/getting-started/working-with-quick/multi-subscriptions.md).

We now discuss implementation details of multi-subscriptions.
As a reminder, we followed these steps:

1. Modify your schema with and define the multi-subscription.
2. Apply the new schema.
3. Run the multi-subscription.
4. Ingest data.


## Apply the new schema

The modification of the schema has no impact
until it is applied.

When applied,
a [MultiSubscriptionFetcher](https://github.com/bakdata/quick/blob/master/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/subscription/MultiSubscriptionFetcher.java)
is created.
During this process, 
Quick constructs a DataFetcherClient (for REST calls)
and a SubscriptionProvider (for consuming from a topic)
for each field marked with the `@topic` directive.

## Run the multi-subscription

When a query is executed, 
the created Kafka Consumers poll the corresponding topics for events.
When a new event is emitted,
it is sent via a WebSocket to the user.
To get the missing part of complex objects, the _MultiSubscriptionFetcher_
uses either the REST service of the corresponding mirror
or an internal cache to fetch missing data.
This choice depends on the current scenario.
In our [example](../user/getting-started/working-with-quick/multi-subscriptions.md),
there are three scenarios for building complex objects via multi subscriptions:

1. Purchase event arrives; there were some click events,
   but they have not been seen by the _MultiSubscriptionFetcher_ yet
   (for example, because the subscription started after the events were produced).
2. Purchase event arrives, and we have already seen the click event for the corresponding id.
3. Purchase event arrives, and there has been no click event.
   In the above considerations, the first event is `Purchase`.
   However, the choice is interchangeable.
   The system behaves similarly
   if the order of arrival changes,
   i.e., `Click` first, then`Purchase`.

## Ingest data

Say you first ingest a single `Purchase`.
Thus, a `Click` is missing.
_MultiSubscriptionFetcher_ first checks the internal cache to see
whether there is a `Click` that refers to the particular id
(the same the `Purchase` refers to).
If successful, _MultiSubscriptionFetcher_ retrieves the value from the cache,
creates a complex object,
and returns it to the user (Scenario 2).
If there is a cache miss,
_MultiSubscriptionFetcher_ uses the REST interface of the corresponding client-mirror
and sends a request for the desired key.
If there were previous click events (Scenario 
1), it retrieves `Click` data,
creates the complex object
and inserts the information into the cache for later use.
If it receives nothing from the REST endpoint,
the result depends on the user's decision concerning null values.
If a user allows null values,
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

If null values are not allowed,
a user will receive a subscription error:
```bash
Error: The field at path '/userStatistics/click' was declared as a non null type,
but the code involved in retrieving data has wrongly returned a null value [...].
```
