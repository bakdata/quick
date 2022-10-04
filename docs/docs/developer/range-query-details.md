# Range queries details

This part of the documentation describes the details of processing range queries.
For the introduction of range queries, see:
[Range queries](https://bakdata.github.io/quick/{{ quick_version }}/user/getting-started/working-with-quick/range-queries/).

In this part, we want to describe what happens under the hood
when users follow the steps for integrating range queries into their applications
(described in the user section).
As a reminder, these steps are:
1. Modify your GraphQL schema and define a range in the query.
2. Apply the schema to the gateway.
3. Configure your topic with the range information.
4. Create and execute the range query.

## Technical Context

Before delving into the details of each step,
we will provide some technical context
for arriving at a better understanding of the range queries.

### Mirrors

Each time you create a new topic in Quick, a corresponding mirror is deployed.
A mirror is a Kafka Streams application that reads the content of a topic
and exposes it through a key-value REST API.
The API is linked with a specific state store. 
A state store in Kafka can either be a persistent state store (by default RocksDB)
or in-memory state store.
Regardless of the type of the chosen state store, their functionality is the same. 
In any case, it's a key-value store, which means that all keys are unique.
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
and there is no possibility to access the earlier value.  
In subsequent parts of this section,
we refer to queries that can only retrieve the latest record as point queries. 
Similarly, if a specific mirror is only capable of supporting such queries,
we say that this is a mirror with a point index.  
Because of the intrinsic nature of state stores, 
providing a possibility to access previous values (making a range query that encompasses more than one value 
associated with `userId=1`) demands a change in the key representation. 

### Introducing possibility to carry out range queries

To circumvent the limitation of a key-value store and be able to perform range queries,
Quick uses an alternative approach to deal with keys. 
Each key is a flattened string with a combination of the topic key and the value
for which the range queries are requested.
The keys are padded (depending on the type `Int` 10 digits or `Long` 19 digits) 
with zeros to keep the lexicographic order.
The general format of the key in the state store is: 
`<zero_paddings><topicKeyValue>_<zero_paddings><rangeFieldValue>`.  
Following the example from the table: If we've a topic with `userId` as its key
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
The flatten key approach creates unique keys for each user with a given rating.
Consequently, all the values will be accessible when running a range query.
In later parts of this section, a mirror that has the capability of supporting range queries
is called a mirror with a range index.
---

## Modify your GraphQL schema and define a range in the query



## Apply the schema to the gateway

Nothing changes here in comparison to point queries.

## Configure your topic with the range information.

When you execute the `topic create` command with the `--range-field`, i.e.:
```shell
quick topic create user-rating-range --key int --value schema --schema example.UserReview --range-field rating
```
a request is sent to the manager, which prepares the deployment of a 
mirror called rating-range.
Two indexes are created behind the scenes:

1. Point index only over the topic key (userId).
2. Range index over the topic key (here, the userId) and rating.

Concerning the point index and point queries,
Quick uses [MirrorProcessor](https://github.com/bakdata/quick/blob/master/mirror/src/main/java/com/bakdata/quick/mirror/MirrorProcessor.java)
and [KafkaQueryService](https://github.com/bakdata/quick/blob/master/mirror/src/main/java/com/bakdata/quick/mirror/service/KafkaQueryService.java)
to insert values to a state store and retrieve them from it, respectively.




## Create and execute the range query.

When you execute a range query, you provide two additional parameters in the entry point.
These attributes define your range. 
