# Range Queries Details

This part of the documentation describes the details of processing Range Queries.

## Kafka State Store

To arrive at a good understanding of Range Queries, it is worth considering how Kafka Streams State Store works
behind the scenes. By default, it uses RocksDB as an internal key-value cache. Since it a key-value store, all keys
are unique, and it is impossible to store different values for the same key. Consider the following entries that
are saved in a topic:

| key (UserId)  | value |
|:-------------:|--------------------|
|  1   | {timestamp: 1, serviceId: 2, requests: 10, success: 8}   |
|  1   | {timestamp: 2, serviceId: 3, requests: 5, success: 3}   |
|  2  | {timestamp: 1, serviceId: 4, requests: 7, success: 2}   |

The table indicates that there are two entries for the `key=1`. The second entry is newer, which means that its value
is the current one in the store. If you query the store with `key=1`,
you get `{timestamp: 2, serviceId: 3, requests: 5, success: 3}` and there is no possibility to access the previous value.
Because of the intrinsic nature of state stores, providing a possibility to access previous values, i.e. making a range query that encompasses
more than one value associated with `key=1` demands a change in the key representation.

## Range Processor for Range Queries




