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

A corresponding mirror is deployed each time you create a new topic in Quick.
A mirror is a Kafka Streams application that reads the content of a topic
and exposes it through a key-value REST API.
The API is linked with a specific state store.
A state store in Kafka can either be a persistent state store (by default RocksDB)
or in-memory state store.
Regardless of the chosen state store type, their functionality is the same.
In any case, it's a key-value store, meaning all keys are unique.
Storing different values for the same key is impossible.
Consider the following entries that are saved in a topic:

| key (UserId) | value                                       |
|:------------:|---------------------------------------------|
|     `1`      | `{userId: 1, purchaseId: "abc", rating: 2}` |
|     `1`      | `{userId: 1, purchaseId: "def", rating: 4}` |
|     `2`      | `{userId: 2, purchaseId: "ghi", rating: 4}` |

The table indicates that there are two entries for the `userId=1`.
The second entry is newer, meaning its value is the current one in the store.
Suppose you query the store with `userId=1`.
In that case, you get `{userId: 1, purchaseId: "def", rating: 4}`,
and there is no possibility of accessing the earlier value.  
In subsequent parts of this section,
we refer to queries that can only retrieve the latest record as point queries.
Similarly, suppose a specific mirror is only capable of supporting such queries. In that case,
we say this is a mirror with a point index.  
Because of the intrinsic nature of state stores,
providing a possibility to access previous values (making a range query that encompasses more than one value
associated with `userId=1`) demands a change in the key representation.

### Introducing the possibility of carrying out range queries

To circumvent the limitation of a key-value store and be able to perform range queries,
Quick uses an alternative approach to deal with keys.
Each key is a flattened string with a combination of the topic key and the value
for which the range queries are requested.
The keys are padded (depending on the type `Int` 10 digits or `Long` 19 digits)
with zeros to keep the lexicographic order.
The general format of the key in the state store is:
<nobr>`<zero_paddings><topicKeyValue>_<zero_paddings><rangeFieldValue>`</nobr>.  
Following the example from the table: If we have a topic with `userId` as its key
and want to create a range over the `rating`,
the key in the state store for the first entry looks like this:
``` 
00000000001_00000000002
```
And for the second:
``` 
00000000001_00000000004
```
Regarding negative values, the minus sign is appended at the beginning of the padded string.
For example, consider a user with the negative (for whatever reason) id number `userId=-10`
and `rating=10`.
Then, the index looks as follows:
``` 
-00000000010_00000000010
```
The flatten-key approach creates unique keys for each user with a given rating.
Consequently, all the values will be accessible when running a range query.
In later parts of this section, a mirror that can support range queries
is called a mirror with a range index.


## Modify your GraphQL schema and define a range in the query

The modification of the schema has no impact
until it is applied.

## Apply the schema to the gateway

When you apply a schema that
contains the topic directive with additional fields
(`rangeFrom` and `rangeTo`),
a [RangeQueryFetcher](https://github.com/bakdata/quick/blob/master/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/RangeQueryFetcher.java)
is created.
This class will be later used
to deliver a result of a range query to the user.

## Configure your topic with the range information

When you execute the `topic create` command with the `--range-field` option,
a request is sent to the Manager.
The Manager prepares the deployment of a mirror, which contains
both [Point Index Processor](https://github.com/bakdata/quick/blob/master/mirror/src/main/java/com/bakdata/quick/mirror/MirrorProcessor.java)
and [Range Index Processor](https://github.com/bakdata/quick/blob/master/mirror/src/main/java/com/bakdata/quick/mirror
/range/MirrorRangeProcessor.java).
Each time a new value is sent to the topic, both processors are called.
The first one creates a new key-value pair
if the specified key does not exist.
If it does, the value for the given key is overwritten (precisely as described above).
If the key exists, but you specify `null` as the value,
the key and the corresponding (previous) value will be deleted from the state store.
The second processor creates the range index in the way that was
discussed above.

## Create and execute the range query

When you prepare a range query,
you provide two additional parameters in the entry point.
These attributes define your range.
After you have executed the query, it hits the gateway.
There, it is processed by the [RangeQueryFetcher](https://github.com/bakdata/quick/blob/master/gateway/src/main/java/com/bakdata/quick/gateway/fetcher/RangeQueryFetcher.java).
_RangeQueryFetcher_ is responsible for extracting the information
about the range from the query you passed.
Having collected the necessary data
(information about the key, the start of the range,
and the end of the range),
the Gateway sends the get request to the mirror
and fetches the result.
