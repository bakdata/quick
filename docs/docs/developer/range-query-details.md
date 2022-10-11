# Range queries details

This part of the documentation describes the details of processing range queries.
For the introduction of range queries, see:
[Range queries](https://bakdata.github.io/quick/{{ quick_version }}/user/getting-started/working-with-quick/range-queries/).

This part outlines what happens under the hood
when users follow the steps for integrating range queries
into their applications (described in the user section).
As a reminder, these steps are:

1. Modify your GraphQL schema and define a range in the query.
2. Apply the schema to the gateway.
3. Configure your topic with the range information.
4. Create and execute the range query.

## Technical Context

Before delving into the details of each step,
a technical context is provided
for arriving at a better understanding of the range queries.

### Mirrors

A corresponding mirror is deployed each time
you create a new topic in Quick.
A mirror is a Kafka Streams application
that reads the content of a topic
and exposes it through a key-value REST API.
The API is linked with a specific state store.
A state store in Kafka can either be a persistent state store
(by default RocksDB) or in-memory state store.
Regardless of the chosen state store type,
their functionality is the same.
In any case, it's a key-value store,
meaning all keys are unique.
Storing different values for the same key is impossible.
Consider the following entries that are saved in a topic:

| key (productId) | value                                             |
|:---------------:|---------------------------------------------------|
|      `123`      | `{productId: 123, name: "T-Shirt", timestamp: 2}` |
|      `123`      | `{productId: 123, name: "T-Shirt", timestamp: 3}` |
|      `234`      | `{productId: 234, name: "Hoodie", timestamp: 4}`  |

The table indicates that there are two entries for the `productId=123`.
The second entry is newer, meaning its value is the current one in the store.
Suppose you query the store with `productId=123`.
In that case, you get `{productId: 123, name: "T-Shirt", timestamp: 3}`,
and there is no possibility of accessing the earlier value.  
In subsequent parts of this section,
we refer to queries that can only retrieve
the latest record as point queries.
Similarly, suppose a specific mirror is only
capable of supporting such queries.
In that case, we say this is a mirror with a point index.  
Because of the intrinsic nature of state stores,
providing a possibility to access previous values
(making a range query that encompasses more than one value
associated with `productId=123`) demands a change in the key representation.

### Introducing the possibility of carrying out range queries

To circumvent the limitation of a key-value store
and be able to perform range queries,
Quick uses an alternative approach to deal with keys.
Each key is a flattened string with a combination of the topic key
and the zero-padded value
for which the range queries are requested.
The values are padded (depending on the type `Int` 10 digits or `Long` 19 digits)
with zeros to keep the lexicographic order.
The general format of the key in the state store is:
<nobr>`<topicKeyValue>_<zero_paddings><rangeFieldValue>`</nobr>.  
Following the example from the table:
If we have a topic with `productId` as its key
and want to create a range over the `timestamp`,
the key in the state store for the first entry looks like this:
``` 
123_00000000002
```
And for the second:
``` 
123_00000000003
```
Regarding negative values, the minus sign is appended
at the beginning of the padded string.
For example, consider a product with the negative (for whatever reason)
id number `productId=-10` and `timestamp=10`.
Then, the index looks as follows:
``` 
-10_00000000010
```
The flatten-key approach creates unique keys for each product with a given timestamp.
Consequently, all the values will be accessible when running a range query.
In later parts of this section, a mirror that can support range queries
is called a mirror with a range index.

## Modify your GraphQL schema and define a range in the query

The modification of the schema has no impact
until it is applied.

## Apply the schema to the gateway

When you apply a schema that
contains the topic directive with the additional fields
(`rangeFrom` and `rangeTo`),
a [`RangeQueryFetcher`](https://github.com/bakdata/quick/blob/c8778ce527575c545a864ccbc3d98e3502fbb2a2/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/RangeQueryFetcher.java)
is created.
This class will be later used
to deliver a result of a range query to the user.

## Configure your topic with the range information

When you execute the `topic create` command with the `--range-field` option,
a request is sent to the Manager.
The Manager prepares the deployment of a mirror, which contains
both [Point Index Processor](https://github.com/bakdata/quick/blob/6fed9f20f237663cc00e3359de92efaf40307f28/mirror/src/main/java/com/bakdata/quick/mirror/point/MirrorProcessor.java)
and [Range Index Processor](https://github.com/bakdata/quick/blob/6fed9f20f237663cc00e3359de92efaf40307f28/mirror/src/main/java/com/bakdata/quick/mirror/range/MirrorRangeProcessor.java).
Each time a new value is sent to the topic,
both processors are called.
The first one creates a new key-value pair
if the specified key does not exist.
If it does, the value for the given key is overwritten
(precisely as described above).
If the key exists, but you specify `null` as the value,
the key and the corresponding (previous) value will be deleted from the state store.
The second processor creates the range index in the way that was
discussed above.

## Create and execute the range query

When you prepare a range query,
you provide two additional parameters in the entry point.
These attributes define your range.
After you have executed the query, it hits the gateway.
There, it is processed by the [`RangeQueryFetcher`](https://github.com/bakdata/quick/blob/c8778ce527575c545a864ccbc3d98e3502fbb2a2/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/RangeQueryFetcher.java).
`RangeQueryFetcher` is responsible for extracting the information
about the range from the query you passed.
Having collected the necessary data
(information about the key, the start of the range,
and the end of the range),
the gateway sends the get request to the mirror
and fetches the result.
