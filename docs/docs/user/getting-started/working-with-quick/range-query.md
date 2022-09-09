# Range Queries

Imagine a scenario where a company wants to analyze purchases a specific customer was unsatisfied with to grant them 
a promo code if the amount exceeds a given value.
To find the number of disappointing purchases, the company could theoretically fetch all entries from the 
appropriate Kafka topic and filter them according to the users and ratings.
However, it would be easier to specify the desired range of ratings considered flawed (from 1 to 4) and receive the 
corresponding records immediately.
<br />
For another example, say you have a product with a unique id and a version number.
With each new release of the product, you update the version number.
You want to check if improving the product (the latest version) led to increased sales.
With the default fetching strategy in Quick (a so-called Point Query), you could only check the number of sold 
pieces with the latest version because the records with the same id are overwritten.
Thus, you don't have access to the entries that refer to earlier versions.
Again, a possible solution would be to fetch all entries and filter them.
However, having the possibility to choose the desired version range and receiving the desired data is more convenient.
<br />
To facilitate such tasks, Quick introduces Range Queries which enable the retrieval of values from a given topic 
according to a specific range of particular fields.

To be able to integrate the Range Queries into your application, you must take the following steps:
1. Deploy a Range Mirror.
2. Define a range in the GraphQL Query type.
3. Execute the query.

The following subsections will describe these steps in detail.
To present the idea of Range Queries, we will extend the schema presented before with the following type:
```graphql title="schema.gql"
type UserReview {
    userId: Int!
    purchaseId: String!
    rating: Int
}
```
Assuming that you have already created a context and a gateway (named `example`), you can send some ratings into 
Quick using the REST API
of the ingest service:
```shell
 curl --request POST --url "$QUICK_URL/ingest/user-rating-range" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./ratings.json"
```
Here is an example of the `ratings.json` file:
```json title="ratings.json"
[
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchaseId" : 123,
            "rating": 7
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchaseId" : 456,
            "rating": 2
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchaseId" : 789,
            "rating": 4
        }
    },
    {
        "key" : 456,
        "value" : {
            "userId" : 456,
            "purchaseId" : 321,
            "rating": 7
        }
    }
]
```
## 1. Deploy a Range Mirror

To use Range Queries, you must create a so-called _Range Mirror_.
A Range Mirror is a special mirror with an index structure that allows the execution of Range Queries.
You can create one using the Quick CLI. You do this by executing the topic creation command, however, with some 
additional options:
```
quick topic create user-rating-range --key int --value schema --schema example.UserReview --rangeField rating --point
```
In comparison to the previous form of the command, you can see two new elements (options): `--rangeField`
and `--point`. <br />
`--rangeField` is an optional field. Specifying it enables you to create a Range Mirror and carry out Range Queries. 
`--rangeField` must be linked with a specific field over which you want your Range Queries to be executed. In the 
example above, the option is linked to the `rating` field. <br />
`--point` is a parameter that tells Quick to use the current mirror implementation to perform so-called Point 
Queries. Point Queries are queries that are executed by default in Quick (thus, you don't have to specify the 
`-point` option explicitly) and return a single value for a given key. <br />
You can also completely drop the possibility of performing Point Queries by providing the `--no-point` option. <br />
`--point` and `--rangeField` are not exclusive. You can execute both Point and Range Queries in your application.

There are some constraints upon the values (values that you provide with the `--value` option) for which Range 
Queries can be executed:
1. The value has to be a complex type, i.e., Avro or Proto. The reason is the Range Index is built over the topic 
   key and a field.
2. The field type over which you want to execute queries has to be a `Long` or `Int`.

When you execute the command (`quick topic ...`), a request is sent to the manager, which prepares
the deployment of a Range Mirror called `rating-range`. This mirror creates two indexes:
1. Range Index over the topic key (here, the `userId`) and `rating`.
2. Point Index only over the topic key (`userId`). <br />
   If you are interested in details of Range Query processing, you can visit ...

## 2. Define a range in the GraphQL Query type

The second step is defining a range in the GraphQL Query type. Using the UserPurchase type mentioned above, you 
could proceed as follows:

```graphql
type Query {
    userRatings(
        userId: Int
        ratingFrom: Int
        ratingTo: Int
    ): [UserRating] @topic(name: "user-rating-range", 
                             keyArgument: "userId", 
                             rangeFrom: "ratingFrom", 
                             rangeTo: "ratingTo")
}

type UserRating {
    userId: Int!
    purchaseId: String!
    rating: Int
}
``` 
The example above indicates that fields of a query refer to the fields over which the Range Index is built.
Thus, the Query definition consists of the key field (`userId`) and two fields that refer to the range.
In this example, the names of fields over which the range is created follow the schema _field**From**_, _field**To**_, 
where _field_ is the field declared in the topic creation command (Step 1).
Please note that following this convention isn't mandatory.
<br />
When you execute a Range Query, you expect to receive a list of entries.
As you can see, the return type of the 
query is a list of _UserRating_.
<br /> 
The last element of the query definition is a topic directive which contains two new elements: `rangeFrom` and 
`rangeTo`.
The values assigned to these two parameters are used to create a desired range for the query.

## 3. Execute the query

Say you want to find the purchases the client with `id=1` was unsatisfied with.
Assuming that a disappointing purchase has a rating lower than 5, you can execute the following query to obtain the 
results.
```graphql
{
    userRatings(userId: 123, ratingFrom: 1, ratingTo: 4)  {
        purchaseId
    }
}
```
Upon successful execution of a query, you should receive the following list of ratings:
```json
{
    "userRatings" : [
        {
            "purchaseId" : 456
        },
        {
            "purchaseId" : 789
        }
    ]
}
```
## Limitations

The following listing describes the limitations of the current Range Queries implementation:

1. It's impossible to define ranges over several fields. 
2. A range can only be defined on a field whose type is `Int` or `Long`.
3. It's impossible to dynamically change the field with which the current Range Mirror is associated.
