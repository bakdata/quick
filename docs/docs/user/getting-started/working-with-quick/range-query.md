# Range queries

We now extend the `Product` type
from the [e-commerce example](query-data.md)
with time information.
This allows a company to analyse
the development of the price over time.
Using this information,
a company could investigate
which factors might have influenced 
the price of a specific product.

The company could fetch all products
and filter them accordingly
to find the desired product's prices in a given period.
However, range queries allow
specifying a product id and a time-range
to retrieve the corresponding records immediately.

To integrate range queries into your application, you must take the following steps:

1. Modify your GraphQL schema and define a range in the query.
2. Apply the schema to the gateway.
3. Configure your topic with the range information.
4. Create and execute the range query as defined in step (1).

## Define a range in the GraphQL query type

To introduce range queries, we will extend the previous schema as follows:
```graphql title="schema.gql"
type Query {
    productPriceInTime(
        productId: Int
        timestampFrom: Int
        timestampTo: Int
    ): [Product] @topic(name: "product-price-range",
        keyArgument: "productId",
        rangeFrom: "timestampFrom",
        rangeTo: "timestampTo")
}

type Product {
    productId: Int!
    name: String
    description: String
    price: Price
    timestamp: Int
}

type Price {
    total: Float
    currency: String
}
```
As you can see, the `Product` type has been extended.
It contains a timestamp that can describe
the price of the product at a given time.

However, the most notable changes are in the `Query` type.
First, the query (`productPriceInTime`) has new fields: `timestampFrom` and `timestampTo`.
Second, the `@topic` directive has changed:
In the query `productPriceInTime`, you declare the two fields that describe your desired range
(here, the timestamp range).
These field values are later assigned to two new parameters of the
`@topic` directive, `rangeFrom` and `rangeTo` respectively.

In our example, `timestampFrom` and `timestampTo` follow the naming scheme _field**From**_ and _field**To**_
where _field_ is the field declared in the topic creation command (see later step 3).
Following this convention is not mandatory.
You can name the parameters that define your range as you wish.
However, we suggest following this pattern to increase readability.

When you execute a range query, you receive a list of entries.
Therefore, the return type of the query is a list of _UserRating_.

!!! Important
    With range queries as described here,
    you can query data using the key of your Kafka messages,
    that is, the topic's key.
    Thus, your data follow a specific format.
    The value of the field chosen for `keyArgument`
    must be the same as the value of the topic key.
    [Below](#execute-the-query), 
    you will notice that the data you send to the topic
    follows this schema. 
    The key of each message is equal to the value's `productId`
    (the field chosen as the `keyArgument`).
    [Later](#range-queries-with-a-value-key-field), we describe how to make range queries using
    one of the value's fields as a key.

## Apply the schema to the gateway

Just like before, you need to apply the modified schema to the gateway as follows:
```shell
quick gateway apply example -f schema.gql
```

## Configure your topic with the range information

To use range queries, you must set the `--range-field` parameter when creating the topic.
Under the hood, Quick creates additional data structures that enable the execution of range queries.
!!! Note
    Because of the change in the `Product` type,
    you must delete the `product` topic
    (if you created it before)
    and create it again.
    To delete the topic,
    use the following command:
    `quick topic delete product`
To create a topic with the new parameter, use the Quick CLI as follows:
```
quick topic create product-price-range --key int --value schema --schema example.Product --range-field timestamp
```

Note that `--range-field` links a particular field you can later use for range queries.
In our example, the `timestamp` field of the `Product` is linked with a range.
The changes in the `Query` described above refer to this field you define here with `--range-field`.

`--range-field` is an optional flag.
If you do not specify it, Quick can solely return values for a given key.
If you specify it, Quick will return values for a given key and a range of desired values.
That is, it executes point queries and range queries.

Note the constraints on the values (which you define via the `--value` option):

1. The value has to be a complex type, i.e., `Avro` or `Proto`.
2. The range field type has to be `Long` or `Int`.

If you are interested in details of the query processing,
visit the developer [section on ranges](../../../developer/range-query-details.md).

## Execute the query

Before executing our range query, you need some data ;)  
You can send products into Quick using [the ingest service](ingest-data.md).

The command below sends products to the `product-price-range` topic.
```shell
 curl --request POST --url "$QUICK_URL/ingest/product-price-range" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./products.json"
```
Here is an example of the `products.json` file:
??? "Example `products.json`"
    ```
    [
      {
        "key": 111,
        "value": {
          "productId": 111,
          "name": "T-Shirt",
          "description": "black",
          "price": {
            "total": 14.99,
            "currency": "DOLLAR"
          },
          "timestamp": 1
        }
      },
      {
        "key": 111,
        "value": {
          "productId": 111,
          "name": "T-Shirt",
          "description": "black",
          "price": {
            "total": 19.99,
            "currency": "DOLLAR"
          },
          "timestamp": 2
        }
      },
      {
        "key": 222,
        "value": {
          "productId": 222,
          "name": "Jeans",
          "description": "Non-stretch denim",
          "price": {
            "total": 79.99,
            "currency": "EURO"
          },
          "timestamp": 1
        }
      },
      {
        "key": 333,
        "value": {
          "productId": 333,
          "name": "Shoes",
          "description": "Sneaker",
          "price": {
            "total": 99.99,
            "currency": "DOLLAR"
          },
          "timestamp": 1
        }
      },
      {
        "key": 111,
        "value": {
          "productId": 111,
          "name": "T-Shirt",
          "description": "black",
          "price": {
            "total": 24.99,
            "currency": "DOLLAR"
          },
          "timestamp": 3
          }
      },
        {
        "key": 222,
        "value": {
          "productId": 222,
          "name": "Jeans",
          "description": "Non-stretch denim",
          "price": {
            "total": 99.99,
            "currency": "EURO"
          },
          "timestamp": 2
          }
        },
        {
        "key": 111,
        "value": {
          "productId": 111,
          "name": "T-Shirt",
          "description": "black",
          "price": {
            "total": 29.99,
            "currency": "DOLLAR"
          },
          "timestamp": 4
          }
      }
    ]
    ```

Let's now find the prices for product `111` in the time-window `1` to `3`.
!!! Note
    The upper bound of a range is exclusive.
    Therefore, we use `timestampTo:4`.
```graphql
query {
  productPriceInTime(productId: 111, timestampFrom: 1, timestampTo: 4) {
    productId,
    price
    {
      total
    }
    timestamp
  }
}
```

Here you go — this is the list of the desired products.
```json
[
  {
    "productId": 111,
    "price": {
      "total": 14.99
    },
    "timestamp": 1
  },
  {
    "productId": 111,
    "price": {
      "total": 19.99
    },
    "timestamp": 2
  },
  {
    "productId": 111,
    "price": {
      "total": 24.99
    },
    "timestamp": 3
  }
]
```

### Range queries with a value key field

Consider the scenario where you have purchases as values with timestamps.
Range queries as described above,
don't let you query records within a time range for a fixed `userId`,
since the range field is not the key of the message.
You can use the `--range-key` option to circumvent this.
The option is set during topic creation
and allows you to specify the new key
for your messages.

Let's change the schema as follows:
```graphql
type Query {
    findUserPurchasesInTime(
        userId: String
        timestampFrom: Int
        timestampTo: Int
    ): [Purchase] @topic(name: "user-purchases",
        keyArgument: "userId"
        rangeFrom: "timestampFrom",
        rangeTo: "timestampTo")
}

type Purchase {
    productId: Int!
    userId: Int!
    amount: Int
    price: Price
    timestamp: Int
}
```
As you can see, the `timestamp` has been added to the `Purchase` type.
The query has also changed.
The new value of `keyArgument` - `userId` - refers
to the field you will define
via `--range-key` in the next step.  
Thus, you must create a new topic
with the `--range-key` parameter
set to that particular value.

```shell
quick topic user-purchases --key string \
 --value schema --schema example.Purchase \
  --range-key userId --range-field timestamp
```
The above command assigns the `userId` from `Purchase`
as `--range-key`.

Use the following snippet,
to send some records to the newly created topic:
```shell
 curl --request POST --url "$QUICK_URL/ingest/user-purchases" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./purchases.json"
```
Here is an example of the `purchases.json` file:
??? "Example `purchases.json`"
    ```
    [
        {
          "key": "abc",
          "value": {
            "productId": 123,
            "userId": 1,
            "amount": 1,
            "price": {
              "total": 19.99,
              "currency": "DOLLAR"
            },
            "timestamp": 1
          }
        },
        {
          "key": "def",
          "value": {
            "productId": 123,
            "userId": 2,
            "amount": 2,
            "price": {
              "total": 30.00,
              "currency": "DOLLAR"
            },
            "timestamp": 2
          }
        },
        {
          "key": "ghi",
          "value": {
            "productId": 456,
            "userId": 2,
            "amount": 1,
            "price": {
              "total": 79.99,
              "currency": "DOLLAR"
            },
            "timestamp": 3
          }
        },
        {
          "key": "jkl",
          "value": {
            "productId": 789,
            "userId": 2,
            "amount": 1,
            "price": {
              "total": 99.99,
              "currency": "DOLLAR"
            },
            "timestamp": 4
          }
        }
    ]
    ```
Let's now find purchases made by the user with the id `2`
within the timeframe between 1 and 3 
(remember that the bound is exclusive).
```graphql
query {
    findUserPurchasesInTime(id: "2", timestampFrom: 1, timestampTo: 4) {
    productId,
    price
    {
        total
    }
  }
}
```

You should see the following result:
```json
[
  {
    "data": {
      "findUserPurchasesInTime": [
        {
          "productId": 123,
          "price": {
            "total": 30
          }
        },
        {
          "productId": 456,
          "price": {
            "total": 79.99
          }
        }
      ]
    }
  }
]
```

## Limitations

The following listing describes the limitations of the current range queries implementation:

1. Defining ranges over several fields isn't supported.
2. A range can only be defined on a field whose type is `Int` or `Long`.
3. Changing the field associated with a given range index isn't supported.
