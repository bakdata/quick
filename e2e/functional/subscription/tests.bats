#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
GATEWAY="subscription-gateway-test"
PRODUCT_TYPE="Product"
PURCHASE_TYPE="Purchase"
USER_TYPE="User"
PRODUCT_TOPIC="subscription-product-topic-test"
PURCHASE_TOPIC="subscription-purchase-topic-test"
USER_TOPIC="subscription-user-topic-test"
PRODUCT_INGEST_URL="${HOST}/ingest/${PRODUCT_TOPIC}"
USER_INGEST_URL="${HOST}/ingest/${USER_TOPIC}"
PURCHASE_INGEST_URL="${HOST}/ingest/${PURCHASE_TOPIC}"
GRAPHQL_URL="https://dev.d9p.io/gateway/${GATEWAY}/graphql"
GRAPHQL_CLI="gql-cli ${GRAPHQL_URL} -H ${API_KEY}"

setup() {
    if [ "$BATS_TEST_NUMBER" -eq 1 ]; then
        printf "creating context for %s\n" "$HOST"
        printf "with API_KEY: %s\n" "${X_API_KEY}"
        quick context create --host "${HOST}" --key "${X_API_KEY}"
    fi
}

@test "should deploy product-gateway" {
  skip
  run quick gateway create ${GATEWAY}
  echo "$output"
  sleep 30
  [ "$status" -eq 0 ]
  [ "$output" = "Create gateway subscription-gateway-test (this may take a few seconds)" ]
}

@test "should apply schema to product-gateway" {
  skip
  run quick gateway apply ${GATEWAY} -f schema.gql
  echo "$output"
  [ "$status" -eq 0 ]
  [ "$output" = "Applied schema to gateway ${GATEWAY}" ]
}

@test "should create product-topic" {
  skip
  run quick topic create "${PRODUCT_TOPIC}" --key-type long --value-type schema --schema "${GATEWAY}.${PRODUCT_TYPE}"
  echo "$output"
  [ "$status" -eq 0 ]
  [ "$output" = "Created new topic ${PRODUCT_TOPIC}" ]
}

@test "should create purchase-topic" {
  skip
  run quick topic create "${PURCHASE_TOPIC}" --key-type string --value-type schema  --schema "${GATEWAY}.${PURCHASE_TYPE}"
  echo "$output"
  [ "$status" -eq 0 ]
  [ "$output" = "Created new topic ${PURCHASE_TOPIC}" ]
}

@test "should create user-topic" {
  skip
  run quick topic create "${USER_TOPIC}" --key-type string --value-type schema  --schema "${GATEWAY}.${USER_TYPE}"
  echo "$output"
  [ "$status" -eq 0 ]
  [ "$output" = "Created new topic ${USER_TOPIC}" ]
}

@test "should ingest data in topics" {
  skip
  curl --request POST --url "${PRODUCT_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./products.json"
  curl --request POST --url "${USER_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./users.json"
}

# TODO: The Graphql CLI tools can not properly test subscriptions. Check on this later.
# https://github.com/hasura/graphqurl#subscriptions
# https://gql.readthedocs.io/en/latest/gql-cli/intro.html
