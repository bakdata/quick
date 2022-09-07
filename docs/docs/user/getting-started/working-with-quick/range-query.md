# Range Queries

This part describes _Range Queries_, which are supported in Quick from version 0.8.

## Introduction

Imagine a scenario where a company wants to find out purchases a specific customer was not satisfied with to grant them 
a promo code if the amount exceeds a given value. To determine the number
of disappointing purchases, the company could theoretically fetch all entries from the appropriate Kafka topic
and then filter them according to the users and ratings. However, it would be easier to be able to specify a desired
range of rating considered bad (say, from 1 to 4) and receive the corresponding records immediately. <br />
For another example, let's say you have a product with a unique id and a version number. With each new release of the
product, you update the version number. Now you want to check if improving the product (new version)
led to increased sales. With the default fetching strategy in Quick (a so-called Point Query),
you could only check the number of sold pieces of the current version.
Again, a possible solution would be to fetch all entries and filter them, but having the possibility to choose the desired
version range and receive the desired data swiftly is more convenient. <br />
To facilitate such tasks, Quick introduces Range Queries which enable
the retrieval of values from a given topic according to a specific range of a particular value.

To be able to integrate the Range Queries into your application, you must take the following steps:
1. Deploy a Range Mirror.
2. Define a range in the GraphQL Query type.
3. Execute the query.

The consecutive subsections will describe these steps in detail. <br />
To present the idea of Range Queries we will extend the schema presented before with the following type:
```graphql title="schema.gql"
type UserReview {
    userId: Int!
    purchaseId: String!
    rating: Int
}
```
Assuming that you have already created a context and a gateway. Using the REST API of the ingest service,
you can send some ratings into Quick:
```shell
 curl --request POST --url "$QUICK_URL/ingest/user-rating-range" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./ratings.json"
```

Here is an example of the `products.json` file:
??? "Example `ratings.json`"
```json title="ratings.json"
   [
      {
      },
      {
      },
      {
      }
   ]
```

[//]: # "TODO: Add an exemplary json with 5 purchases whose timestamp span 3 months. Moreover, add a command that
enable the ingestion of the data"

## 1. Deploy a Range Mirror

To use Range Queries, you must create a so-called _Range Mirror_.
A Range Mirror is a special mirror with an index structure that allows the execution of Range Queries.
You can create one using the Quick CLI. You do this by executing the topic creation command, however,
with some additional options:
```
quick topic create user-rating-range --key string --value schema --schema example.UserReview --rangeField rating --point
```
In comparison to the previous form of the command, you can see two new elements (options) here: `--rangeField`
and `--point`. <br />
`--rangeField` is an optional field. Specifying it enables you to create a Range Mirror and carry out Range Queries.
`--rangeField` must be linked with a specific field over which you want your Range Queries to be executed. In the example above,
the option is linked to the `rating` field. <br />
`--point` is a parameter that tells Quick to use the current mirror implementation to perform Point Queries.
By default, Quick creates Point Index. Thus, you don't have to specify the `-point` option explicitly. You can also completely drop
the possibility of performing Point Queries by providing the `--no-point` option.

There are some constraints upon the values (values that you provide with the `--value` option)
for which Range Queries can be executed:
1. The value has to be a complex type, i.e., Avro or Proto. The reason is the Range Index built over
   the topic key and a field.
2. The field type over which you want to execute queries has to be a `Long` or `Int`.

When you execute the command (`quick topic ...`), a request is sent to the manager, which prepares
the deployment of a Range Mirror called `rating-range`. This mirror creates two indexes:
1. Range Index over the topic key (here the `userId`) and `rating`.
2. Point Index only over the topic key (`userId`). <br />
   Range Processor for Mirrors section provides more details about these indices.


## 2. Define a range in the GraphQL Query type

The second step is defining a range in the GraphQL Query type. Using the UserPurchase type mentioned above,
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
The example above indicates that fields of a query refer to the fields over which the Range Index is built.
Thus, the Query definition consists of the key field (`userId`) and two fields that refer to the range.
The names of fields over which the range is created follow the schema _field**From**_, _field**To**_,
where _field_ is the field declared in the topic creation command (Step 1). <br />
When you execute a Range Query, you expect to receive a list of entries. In the example, the return type of the query
is a list of _UserRating_. <br /> 
The last element of the query definition is a topic (the same that you defined in the first step).

## 3. Execute the query

Say you want to find the purchases the client with `id=1` was unsatisfied with.  
Assuming that a disappointing purchase is one that has a rating lower than 5, you can execute the following
query to obtain the results.
```graphql
{
    userRequests(userId: 1, ratingFrom: 1, ratingTo: 4)  {
        purchaseId
    }
}
```
Upon successful execution of a query, you should receive the list of purchase ids, which enables you to count
the total amount of purchases made by the client within the given timeframe.

## FAQ

The following listing contains several questions about Range Queries that might arise to you.

**Q**: Is it possible to define ranges over several fields? <br />
**A**: Currently, this is not supported.

**Q**: Can I define a range over a complex field, i.e., a field defined by the `type` keyword? <br />
**A**: Currently, only the field with the `Int` and `Long` types are supported.

**Q**: Do I have to create a mirror and a range mirror separately? <br />
**A**: You just have to execute the topic command once. The different endpoints for both Point Queries and Range Queries will be created behind the scenes.

**Q**: Can I dynamically change the field with which the Range Mirror is associated? <br />
**A**: No, changing the field that was once assigned is impossible. You must create a new Range Mirror if you
want to change the target field.



