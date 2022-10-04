# Range queries

We now extend [the e-commerce example](query-data.md) with user ratings.
Thus, users can then rank their purchases.
This allows the company to find purchases that did not satisfy customers.
It could then provide promo codes to the unhappy ones.

To find disappointing purchases, the company could fetch all purchases and filter them accordingly.
However, range queries allow to specify a certain range of bad ratings
(say, from 1 to 4 on a 10 point grading scale)
and receive the corresponding records immediately.  

To integrate range queries into your application, you must take the following steps:

1. Configure your topic with the range information.
2. Modify your GraphQL schema and define a range in the query.
3. Apply the schema to the gateway.
4. Create and execute the range query as defined in step (2).

___

NOTE CB This does not make sense here: Consider the following scenario.

The following subsections describe these steps in detail.
To present the idea of range queries, we will extend the schema presented before with the following type:
```graphql title="schema.gql"
type UserReview {
    userId: Int!
    purchase: Purchase!
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
            "purchase" : {
                "purchaseId": "abc",
                "productId": 123,
                "userId": 123,
                "amount": 10,
                "price": {
                  "total": 9.99,
                  "currency": "DOLLAR"
                }
            },
            "rating": 7
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchase" : {
                "purchaseId": "def",
                "productId": 101,
                "userId": 123,
                "amount": 1,
                "price": {
                  "total": 119.99,
                  "currency": "DOLLAR"
                }
            },
            "rating": 2
        }
    },
    {
        "key" : 123,
        "value" : {
            "userId" : 123,
            "purchase" : {
                "purchaseId": "ijk",
                "productId": 333,
                "userId": 123,
                "amount": 2,
                "price": {
                  "total": 19.99,
                  "currency": "DOLLAR"
                }
            },
            "rating": 4
        }
    },
    {
        "key" : 456,
        "value" : {
            "userId" : 456,
            "purchase" : {
                "purchaseId": "fgh",
                "productId": 1234,
                "userId": 456,
                "amount": 5,
                "price": {
                  "total": 29.99,
                  "currency": "DOLLAR"
                }
            },
            "rating": 7
        }
    }
]
```

___

## Configure your topic with the range information

To use range queries, you must set the `--range-field` parameter when creating the topic.
Under the hood, Quick then creates additional data structures that enables the execution of range queries.
Use the Quick CLI as follows:

```
quick topic create user-rating-range --key int --value schema --schema example.UserReview --range-field rating
```

Note that `--range-field` links a certain field you can later use for range queries.
In our example we link the `rating` field.
`--range-field` is an optional flag.
If you do not specify it, Quick can solely return values for a given key.
If you do specify it, Quick will return values for a given key and a range of desired values.
That is, it executes point queries and range queries.

Note the constraints on the values (which you define via the `--value` option):

1. The value has to be a complex type, i.e., `Avro` or `Proto`.
2. The range field type has to be `Long` or `Int`.

If you are interested in details of the query processing,
visit the developer [section on ranges](https://bakdata.github.io/quick/latest/developer/range-queries-details/).

## Define a range in the GraphQL query type

Now let the GraphQL query know how to connect to the topic you just created.
Using the UserPurchase type mentioned above (CB ?!?), you could proceed as follows:

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
    purchase: Purchase!
    rating: Int
}
``` 

In the example above some fields of the query refer to the fields which can be used for range queries.
The query definition consists of the key field (`userId`) and two range fields:

The query definition consists of the key field (`userId`) and two range fields:
`ratingFrom` and `ratingTo`.


In our example `ratingFrom` and `ratingTo` follow the naming scheme _field**From**_ and _field**To**_ where where _field_ is the field declared in the topic creation command (Step 1).
Following this convention is not mandatory.
However, we think it increases readability.

When you execute a range query, you receive a list of entries.
Therefore, the return type of the query is a list of _UserRating_. 

Finally, the query definition's topic directive contains two new elements:
`rangeFrom` and `rangeTo` linking  the new fields `ratingFrom` and `ratingTo` respectively.

These parameter values define the desired range field, here `rating`.

## Apply the schema to the gateway.

Just like before, we now apply this modified schema to tha gateway as follows.

DN tbd.

## Execute the query

Say you want to find the purchases the client with `id=1` was unsatisfied with.
Assuming that a disappointing purchase has a rating lower than 5,
you can execute the following query to obtain the results.
```graphql
{
    userRatings(userId: 123, ratingFrom: 1, ratingTo: 4)  {
        purchaseId
        productId
        price {
           total
        }
    }
}
```
Upon successful execution of a query, you should receive the following list of ratings:
```json
{
    "userRatings" : [
       {
          "purchaseId": "def",
          "productId": 101,
          "amount": 1,
          "price": {
             "total": 119.99
          }
       },
       {
          "purchaseId": "ijk",
          "productId": 333,
          "amount": 2,
          "price": {
             "total": 19.99
          }
       }
    ]
}
```
## Limitations

The following listing describes the limitations of the current range queries implementation:

1. Defining ranges over several fields isn't supported. 
2. A range can only be defined on a field whose type is `Int` or `Long`.
3. Changing the field which is associated with a given range index isn't supported.
