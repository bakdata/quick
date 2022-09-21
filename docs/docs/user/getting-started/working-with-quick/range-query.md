# Range queries

Imagine a scenario where a company wants to analyze the purchases a specific customer was unsatisfied with.
If the amount exceeds a specific value, the customer is given a promo code.
To find the number of disappointing purchases, the company could theoretically fetch all entries
from the appropriate Kafka topic and filter them according to the users and ratings.
However, it would be easier to specify the desired range of ratings considered flawed
(say, from 1 to 4 on a 10 point grading scale) and receive the corresponding records immediately.  
For another example, say you have a product with a unique id and a version number.
With each new release of the product, you update the version number. You want to check if improving the product
(the latest version) led to increased sales.
With the default fetching strategy in Quick (a so-called point query),
you could only check the number of sold pieces with the latest version
because the records with the same id are overwritten.
Thus, you don't have access to the entries that refer to earlier versions.
Again, a possible solution would be to fetch all entries and filter them.
However, having the possibility to choose the desired version range and receiving the desired data is more convenient. 
To facilitate such tasks, Quick introduces range queries which enable the retrieval of values from a given topic 
according to a specific range of particular fields.

To be able to integrate the range queries into your application, you must take the following steps:

1. Deploy a mirror with a range index.  
2. Define a range in the GraphQL query type.  
3. Execute the query.  

The following subsections will describe these steps in detail.
To present the idea of range queries, we will extend the schema presented before with the following type:
```graphql title="schema.gql"
type UserReview {
    userId: Int!
    purchaseId: String!
    rating: Int
}
```
Assuming that you have already created a context and a gateway (named `example`),
you can send some ratings into Quick using the REST API of the ingest service:
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
            "purchaseId" : "123",
            "rating": 7
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchaseId" : "456",
            "rating": 2
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchaseId" : "789",
            "rating": 4
        }
    },
    {
        "key" : 456,
        "value" : {
            "userId" : 456,
            "purchaseId" : "321",
            "rating": 7
        }
    }
]
```
## Deploy a mirror with a range index

To use range queries, you must create a mirror with a range index.
A range index is a structure that enables the execution of range queries.
You can create one using the Quick CLI. You do this by executing the topic creation command
with an additional option:
```
quick topic create user-rating-range --key int --value schema --schema example.UserReview --range-field rating
```
Compared to the previous command form,
you can see a new element (option): `--range-field`.  
Specifying it enables you to create a range index
and consequently carry out range queries.
`--range-field` must be linked with a particular field
over which you want your range queries to be executed.
The option is related to the `rating` field in the example above.  
`--range-field` is an optional flag. When you don't specify it,
the manager only creates a point index (exactly as before).
A point index is a structure for carrying out point queries
(a single value for a given key).
Thus, you can execute either only point queries
or both point and range queries in your application.

There are some constraints upon the values (values that you provide with the `--value` option)
for which range queries can be executed:

1. The value has to be a complex type, i.e., Avro or Proto. The reason is the range index is built over the topic 
   key and a field.  
2. The field type over which you want to execute queries has to be a `Long` or `Int`.

When you execute the command (`quick topic ...`), a request is sent to the manager,
which prepares the deployment of a mirror called `rating-range` with a range index.
Two indexes are created behind the scenes:

1. Range index over the topic key (here, the `userId`) and `rating`.  
2. Point index only over the topic key (`userId`).

If you are interested in details of range query processing,
you can visit [Range queries details](https://bakdata.github.io/quick/0.7/developer/range-queries-details/)
in the Developer guide section.

## Define a range in the GraphQL query type

The second step is defining a range in the GraphQL query type. Using the UserPurchase type mentioned above,
you could proceed as follows:

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
The example above indicates that fields of a query refer to the fields over which the range index is built.
Thus, the query definition consists of the key field (`userId`) and two fields that refer to the range.
In this example, the names of fields over which the range is created follow the schema _field**From**_, _field**To**_, 
where _field_ is the field declared in the topic creation command (Step 1).
Please note that following this convention isn't mandatory.  
When you execute a range query, you expect to receive a list of entries.
As you can see, the return type of the query is a list of _UserRating_.  
The last element of the query definition is a topic directive which contains two new elements: `rangeFrom` and 
`rangeTo`.
The values assigned to these two parameters are used to create a desired range for a specific field.

## Execute the query

Say you want to find the purchases the client with `id=1` was unsatisfied with.
Assuming that a disappointing purchase has a rating lower than 5,
you can execute the following query to obtain the results.
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
            "purchaseId" : "456"
        },
        {
            "purchaseId" : "789"
        }
    ]
}
```
## Limitations

The following listing describes the limitations of the current range queries implementation:

1. Defining ranges over several fields isn't supported. 
2. A range can only be defined on a field whose type is `Int` or `Long`.
3. Changing the field which is associated with a given range index isn't supported.
