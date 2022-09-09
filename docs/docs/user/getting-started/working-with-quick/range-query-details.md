# Range Queries Details

This part of the documentation describes the details of processing Range Queries.

## Kafka State Store

To arrive at a good understanding of Range Queries, it's worth considering how Kafka Streams State Store works
behind the scenes. By default, it uses RocksDB as an internal key-value cache. Since it's a key-value store, all 
keys are unique, and storing different values for the same key is impossible. Consider the following entries that 
are saved in a topic:

| key (UserId)  | value |
|:-------------:|--------------------|
|  1   | {timestamp: 1, serviceId: 2, requests: 10, success: 8}   |
|  1   | {timestamp: 2, serviceId: 3, requests: 5, success: 3}   |
|  2  | {timestamp: 1, serviceId: 4, requests: 7, success: 2}   |

The table indicates that there are two entries for the `key=1`. The second entry is newer, meaning its value is the 
current one in the store. Suppose you query the store with `key=1`. In that case, you get `{timestamp: 2, serviceId: 
3, requests: 5, success: 3}`, and there is no possibility to access the earlier value. Because of the intrinsic 
nature of state stores, providing a possibility to access previous values (making a range query that 
encompasses more than one value associated with `key=1`) demands a change in the key representation. 

## Range Processor for Range Queries

To circumvent the limitation of a key-value store and be able to perform Range Queries, Quick proposes a new approach 
to deal with keys. Each key is a flattened string with a combination of the topic key and the value for which the 
range queries are requested. The keys are padded (depending on the type `Int` 11 digits or `Long` 20 digits) with 
zeros to keep the lexicographic order. The general format of the key in the state store is: 
`<zero_paddings><topicKeyValue>_<zero_paddings><rangeFieldValue>`. <br /> 
Following the example from the table: If we've a topic with `userId` as its key and want to create a range over the 
`timestamp`, the key in the state store for the first entry looks like this:
``` 
00000000001_00000000001
```
And for the second:
``` 
00000000001_00000000002
```
The flatten key approach creates unique keys for each user with a given timestamp. Consequently, all the values will 
be accessible when running a range query.
