# Query data

To query your data, you have to tell Quick how to connect your schema with your topics.
Quick uses GraphQL directives to represent this information.
You can imagine them like annotations and attributes in programming languages:
They add meta information to a schema element form which Quick derives extra functionalities.
The [`@topic` directive](../reference/graphql-extensions#topic) is the most important when working with Quick.
It connects elements in your schema to Apache Kafka topics.

## Query by key

In the first step, you connect the Query field `findPurchase` from the gateway schema to the `purchase` topic.
You can simply change the [existing schema](gateway.md#the-graphql-schema):
```graphql title="schema.gql"
type Query {
    findPurchase(purchaseId: String): Purchase @topic(name: "purchase", keyArgument: "purchaseId")
}

# The rest of the schema remains unchanged 
```

This tells Quick to look up purchases in the `purchase` topic.
The `keyArgument` defines that it should return the purchase with the key specified by the argument `purchaseId`.

You have to re-apply the schema to update Quick:
```shell
quick gateway apply example -f schema.gql
```
---

You can now query the topic.
In a GraphQL query, you must select all fields you want to retrieve recursively.
This query starts with the `findPurchase` field and passes an argument for the `purchaseId`.
It then selects the fields of `Purchase` that the gateway should return,
including the nested type `Price`'s fields.
In this example, the query drops the fields `purchaseId` and `userId`.
```graphql title="find-purchase-query.gql"
{
    findPurchase(purchaseId: "abc") {
        productId
        amount
        price {
            total
            currency
        }
    }
}
```
You can run the query with `gql-cli` and optionally pipe it to `jq`:
```shell
gql-cli "$QUICK_URL/gateway/example/graphql" \
  -H "X-API-Key:$QUICK_API_KEY" \
  < find-purchase-query.gql
```
This command should return a JSON object of the data:
```json
{
  "findPurchase": {
    "productId": 123,
    "amount": 1,
    "price": {
      "total": 19.99,
      "currency": "DOLLAR"
    }
  }
}
```


## Query lists

Quick also lets you query a list of elements.
You can add the following `allPurchases` field to the query:
```graphql title="schema.gql"
type Query {
    findPurchase(purchaseId: String): Purchase @topic(name: "purchase", keyArgument: "purchaseId")
    allPurchases: [Purchase!] @topic(name: "purchase")
}

# The rest of the schema remains unchanged 
```

`allPurchases` differs from `findPurchase` in two ways.
First, it doesn't have an argument and no `keyArgument` specified in the `@topic` directive.
Second, it returns `[Purchase!]` instead of `Purchase`.
The brackets indicate a list of purchases that aren't null because of the exclamation mark.

After applying the schema again, you can also query all purchases:
```graphql title="all-purchases-query.gql"
{
    allPurchases {
        productId
        userId
    }
}
```
```shell
gql-cli "$QUICK_URL/gateway/example/graphql" \
  -H "X-API-Key:$QUICK_API_KEY" \
  < all-purchases-query.gql
```

## Query connected topics

As you may have noticed,
there is a relationship between `Purchase` and `Product` in that `Purchase` has a field `productId`.
What if you want to have the information for the product when querying a purchase?
Quick supports this also through the `@topic` directive.

```graphql title="schema.gql" linenums="1"  
# The query remains unchanged 

type Purchase {
    purchaseId: String!
    productId: Int!
    userId: Int!
    amount: Int
    price: Price
    product: Product @topic(name: "product", keyField: "productId")
}

# The rest of the schema remains unchanged 
```

The last line is new: The `Purchase` now has a new field `product` of Type `Product`.
The directive uses the `keyField` argument to define their relationship:
The `productId` holds the value that Quick should resolve through the product topic.

---

With this, you can query a purchase and directly access the related product information:
```graphql title="purchase-with-product-query.gql"
{
    findPurchase(purchaseId: "abc") {
        amount
        price {
            total
            currency
        }
        product {
            productId
            name
            description
        }
    }
}
```
```shell
gql-cli "$QUICK_URL/gateway/example/graphql" \
  -H "X-API-Key:$QUICK_API_KEY" \
  < purchase-with-product-query.gql
```

This query returns the following data:
```json
{
  "findPurchase": {
    "amount": 1,
    "price": {
      "total": 19.99,
      "currency": "DOLLAR"
    },
    "product": {
      "productId": 123,
      "name": "T-Shirt",
      "description": "black"
    }
  }
}
```

