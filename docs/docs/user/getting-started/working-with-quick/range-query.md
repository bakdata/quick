# Range queries

We now extend the [e-commerce example](query-data.md) with user ratings.
Thus, users can then rank their purchases.
This allows the company to find purchases that did not satisfy customers.
It could then provide promo codes to the unhappy ones.

The company could fetch all purchases and filter them accordingly to find disappointing purchases.
However, range queries allow you to specify a specific range of bad ratings
(say, from 1 to 4 on a 10-point grading scale)
and receive the corresponding records immediately.

To integrate range queries into your application, you must take the following steps:

1. Modify your GraphQL schema and define a range in the query.
2. Apply the schema to the gateway.
3. Configure your topic with the range information.
4. Create and execute the range query as defined in step (1).

## Define a range in the GraphQL query type

To introduce range queries, we will extend the previous schema as follows:
```graphql title="schema.gql"
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
    purchase: Purchase @topic(name: "purchase", keyField: "purchaseId")
    rating: Int
}
```
Let's start with the new type called `UserRating`.
It describes a numerical rating a given user assigns
to a specific purchase previously made (identified by `purchaseId`).

However, the most notable changes are in the `Query` type.
First, (`userRatings`) has new fields: `ratingFrom` and `ratingTo`.
Second, the `@topic` directive has changed:
In the query `userRatings`, you declare the two fields that describe your desired range
(here, the rating range).
These field values are later assigned to two new parameters of the
`@topic` directive, `rangeFrom` and `rangeTo` respectively.

In our example, `ratingFrom` and `ratingTo` follow the naming scheme _field**From**_ and _field**To**_
where _field_ is the field declared in the topic creation command (see later step 3).
Following this convention is not mandatory.
You can name the parameters that define your range as you wish.
However, we think that following this pattern increases readability.

When you execute a range query, you receive a list of entries.
Therefore, the return type of the query is a list of _UserRating_.

## Apply the schema to the gateway

Just like before, you need to apply the modified schema to the gateway as follows:
```shell
quick gateway apply example -f schema.gql
```

## Configure your topic with the range information

To use range queries, you must set the `--range-field` parameter when creating the topic.
Under the hood, Quick creates additional data structures that enable the execution of range queries.
Use the Quick CLI as follows:
```
quick topic create user-rating-range --key int --value schema --schema example.UserRating --range-field rating
```

Note that `--range-field` links a particular field you can later use for range queries.
In our example, the `rating` field of the `UserRating` is linked with a range.
Tha changes in the `Query` described above refer to this field you define here with `--range-field`.

`--range-field` is an optional flag.
If you do not specify it, Quick can solely return values for a given key.
If you specify it, Quick will return values for a given key and a range of desired values.
That is, it executes point queries and range queries.

Note the constraints on the values (which you define via the `--value` option):

1. The value has to be a complex type, i.e., `Avro` or `Proto`.
2. The range field type has to be `Long` or `Int`.

If you are interested in details of the query processing,
visit the developer [section on ranges](https://bakdata.github.io/quick/latest/developer/range-queries-details/).

## Execute the query

Before executing our range query, we need some data ;)  
You can send purchases and ratings into Quick using [the ingest service](ingest-data.md).
If you followed the previous parts of this guide,
you should already have data in the `purchase` topic.
If you didn't, please complete the [section about ingesting data](ingest-data.md)
and add some purchases:

The command below sends ratings to the `user-rating-range` topic.
```shell
 curl --request POST --url "$QUICK_URL/ingest/user-rating-range" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./ratings.json"
```
Here is an example of the `ratings.json` file:
??? "Example `ratings.json`"
    ``` 
    [
      {
        "key": 1,
        "value": {
          "userId": 1,
          "purchaseId": "abc",
          "rating": 7
        }
      },
      {
        "key": 2,
        "value": {
          "userId": 2,
          "purchaseId": "def",
          "rating": 2
        }
      },
      {
        "key": 2,
        "value": {
          "userId": 2,
          "purchaseId": "ghi",
          "rating": 6
        }
      },
      {
        "key": 2,
        "value": {
          "userId": 2,
          "purchaseId": "jkl",
          "rating": 1
        }
      }
    ]
    ```
Let's now find purchases the client with `userId=2` was unsatisfied with.
Assuming that a disappointing purchase has a rating lower than 5,
you can execute the following query to obtain the results.
```graphql
query {
    userRatings(userId: 2, ratingFrom:1, ratingTo:4) {
        userId
        rating
        purchase {
            purchaseId
            productId
            price {
                total
                currency
            }
        }
    }
}
```
Here you go - this is the list of poorly rated purchases.
```json
[
  {
    "userId": 2,
    "rating": 2,
    "purchase": {
      "purchaseId": "def",
      "productId": 123,
      "price": {
        "total": 30,
        "currency": "DOLLAR"
      }
    }
  },
  {
    "userId": 2,
    "rating": 4,
    "purchase": {
      "purchaseId": "jkl",
      "productId": 456,
      "price": {
        "total": 99.99,
        "currency": "DOLLAR"
      }
    }
  }
]
```
## Limitations

The following listing describes the limitations of the current range queries implementation:

1. Defining ranges over several fields isn't supported.
2. A range can only be defined on a field whose type is `Int` or `Long`.
3. Changing the field associated with a given range index isn't supported.