# Multi-subscriptions details

This part of the documentation describes the implementation details
of multi-subscriptions.
For the introduction of multi-subscriptions, see:
[Multi-subscriptions](https://bakdata.github.io/quick/{{ quick_version }}
/user/getting-started/working-with-quick/multi-subscriptions/).

## Creating complex objects

For this section,
we will adhere to the example from the user-guide
(see link above). The user-guide section describes
a scenario in which we have a subscription for a type
whose elements are stored in two separate Kafka topics.
The documentation describes that upon arrival
of an event of a specific type,
the program retrieves the other type immediately.
However, it does explain what happens behind the scenes.

When a multi-subscription is created,
the gateway constructs a separate Kafka Consumer for each field
marked with the `@topic` directive. The created Kafka Consumers
poll the corresponding topics for events. When a new event is emitted,
the event is sent via WebSocket to the end user. However, we only have
a part of the complex object we want to deliver to the user.
To get the missing part of the object, the multi subscription
uses either the REST service of the corresponding mirror
or an internal cache to fetch missing
data and create a complex object immediately.
The use of the specific entity is dependent on the current scenario.
There are three scenarios for building complex objects via multi subscriptions:
1. Purchase event arrives; there were some click events,
   but we have not seen them yet
   (for example, because the subscription started after the events were produced).
2. Purchase event arrives, and we have already seen the click event for the corresponding id.
3. Purchase event arrives, and there has been no click event.
   Note that in the above considerations, the first event is `Purchase`.
   Note, however, that the choice is arbitrary.
   The system would behave in the same fashion
   if you switched the order of arrival,
   i.e., `Click` and not `Purchase` first.

### What happens when a new event arrives

Let's say we have `Purchase` at hand and are missing a `Click`.
We first check the internal cache to see whether there is a `Click` that refers to
the particular id (the same the `Purchase` relates to). If a cache hits,
we retrieve the value, create a complex object, and return it to the user (Scenario 2).
If we have a cache miss, we use the REST interface of the corresponding client-mirror
and send a request for the desired key. We would retrieve click data if there were previous click events (Scenario 1). We create a complex object and put the information in the cache
for later use. If we receive nothing from the REST endpoint,
the result depends on the user's decision concerning null values.
If a user permits the delivery of a null value, an incomplete object will be returned, i.e.:
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
On the other hand, if null values are not allowed, a user will receive a subscription error:
```bash
Error: The field at path '/userStatistics/click' was declared as a non null type,
but the code involved in retrieving data has wrongly returned a null value [...].
```




