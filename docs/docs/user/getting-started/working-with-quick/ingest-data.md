# Ingest data

With the topics created, you can now move data into Quick.
For that, Quick offers a REST API.

You can find the REST API of the ingest service under `$QUICK_URL/ingest`.
You send a `POST` request to `$QUICK_URL/ingest/product` with a JSON list of key-value pairs in the body.
As with the gateway, you also have to set the `X-API-Key` header to `QUICK_API_KEY`.
Using curl, you can create new products like this:
```shell
 curl --request POST --url "$QUICK_URL/ingest/product" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./products.json"
```

Here is an example of the `products.json` file:
??? "Example `products.json`"
    ```json title="products.json"
    [
      {
        "key": 123,
        "value": {
          "productId": 123,
          "name": "T-Shirt",
          "description": "black",
          "price": {
            "total": 19.99,
            "currency": "DOLLAR"
          }
        }
      },
      {
        "key": 456,
        "value": {
          "productId": 456,
          "name": "Jeans",
          "description": "Non-stretch denim",
          "price": {
            "total": 79.99,
            "currency": "EURO"
          }
        }
      },
      {
        "key": 789,
        "value": {
          "productId": 789,
          "name": "Shoes",
          "description": "Sneaker",
          "price": {
            "total": 99.99,
            "currency": "DOLLAR"
          }
        }
      }
    ]
    ```

As explained in [topics](topics.md#creating-new-topics), Quick enforces data to conform to the defined types.
For example, the following product is invalid because the price isn't a complex type:
```json title="invalid-product.json"
{
  "key": 456,
  "value": {
    "name": "T-Shirt",
    "description": "black",
    "price": 19.99
  }
}
```

When you try to ingest it, the ingest service will throw an error:
```shell
curl --request POST --url "$QUICK_URL/ingest/product" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./invalid-product.json"
```

---

You can now also ingest data for purchases:
```shell
 curl --request POST --url "$QUICK_URL/ingest/purchase" \
  --header "content-type:application/json" \
  --header "X-API-Key:$QUICK_API_KEY"\
  --data "@./purchases.json"
```

Use the following example of the `purchases.json` file as an example:
??? "Example `purchases.json`"
    ```json title="purchases.json"
    [
      {
        "key": "abc",
        "value": {
          "purchaseId": "abc",
          "productId": 123,
          "userId": 1,
          "amount": 1,
          "price": {
            "total": 19.99,
            "currency": "DOLLAR"
          }
        }
      },
      {
        "key": "def",
        "value": {
          "purchaseId": "def",
          "productId": 123,
          "userId": 2,
          "amount": 2,
          "price": {
            "total": 30.00,
            "currency": "DOLLAR"
          }
        }
      },
      {
        "key": "ghi",
        "value": {
          "purchaseId": "ghi",
          "productId": 456,
          "userId": 2,
          "amount": 1,
          "price": {
            "total": 79.99,
            "currency": "DOLLAR"
          }
        }
      },
      {
        "key": "jkl",
        "value": {
          "purchaseId": "jkl",
          "productId": 789,
          "userId": 2,
          "amount": 1,
          "price": {
            "total": 99.99,
            "currency": "DOLLAR"
          }
        }
      }
    ]
    ```
