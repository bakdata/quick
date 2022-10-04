# Range queries details

This part of the documentation describes the details of processing range queries.
For the introduction of range queries, see:
[Range queries](https://bakdata.github.io/quick/{{ quick_version }}/user/getting-started/working-with-quick/range-queries/).

## Technical Context

### Mirrors

To arrive at a good understanding of range queries,
it's worth considering how Kafka Streams state store works behind the scenes.
A state store in Kafka can either be a persistent state store (by default RocksDB)
or in-memory state store.
Regardless of the type of the chosen state store, their functionality is the same. 
In any case, it's a key-value store, which means that all keys are unique,
and storing different values for the same key is impossible.
Consider the following entries that are saved in a topic:

| key (UserId) | value                                                  |
|:------------:|--------------------------------------------------------|
|      `1`       | `{timestamp: 1, serviceId: 2, requests: 10, success: 8}` |
|      `1`       | `{timestamp: 2, serviceId: 3, requests: 5, success: 3}`  |
|      `2`       | `{timestamp: 1, serviceId: 4, requests: 7, success: 2}`  |

The table indicates that there are two entries for the `key=1`. The second entry is newer,
meaning its value is the current one in the store.
Suppose you query the store with `key=1`. In that case, you get `{timestamp: 2, serviceId: 
3, requests: 5, success: 3}`, and there is no possibility to access the earlier value.
Because of the intrinsic nature of state stores, 
providing a possibility to access previous values (making a range query that encompasses more than one value 
associated with `key=1`) demands a change in the key representation. 

### Query Processors

Point query processors ... implemented here ...

Range query processors ... implemented here ... in short they do the following:

To circumvent the limitation of a key-value store and be able to perform range queries,
Quick uses an alternative approach to deal with keys. 
Each key is a flattened string with a combination of the topic key and the value
for which the range queries are requested.
The keys are padded (depending on the type `Int` 10 digits or `Long` 19 digits) 
with zeros to keep the lexicographic order.
The general format of the key in the state store is: 
`<zero_paddings><topicKeyValue>_<zero_paddings><rangeFieldValue>`.  
Following the example from the table: If we've a topic with `userId` as its key
and want to create a range over the `timestamp`,
the key in the state store for the first entry looks like this:
``` 
00000000001_00000000001
```
And for the second:
``` 
00000000001_00000000002
```
Regarding negative values, the minus sign is appended at the begging of the padded string.
For example, consider a user with the negative (for whatever reason) id number `userId=-10`
and `timestamp=10`. Then, the index looks as follows:
``` 
-00000000010_00000000010
```
The flatten key approach creates unique keys for each user with a given timestamp.
Consequently, all the values will be accessible when running a range query.

## Configure your topic with the range information.

tbd.

## Modify your GraphQL schema and define a range in the query.

tbd.