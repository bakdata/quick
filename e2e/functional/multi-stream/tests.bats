#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
GATEWAY="multi-stream-gateway-test"
PRODUCT_TYPE="Product"
PURCHASE_TYPE="Purchase"
PRODUCT_TOPIC="multi-stream-product-topic-test"
PURCHASE_TOPIC="multi-stream-purchase-topic-test"
PRODUCT_INGEST_URL="${HOST}/ingest/${PRODUCT_TOPIC}"
PURCHASE_INGEST_URL="${HOST}/ingest/${PURCHASE_TOPIC}"
GRAPHQL_URL="${HOST}/gateway/${GATEWAY}/graphql"
GRAPHQL_CLI="gql-cli ${GRAPHQL_URL} -H ${API_KEY}"


setup() {
    if [ "$BATS_TEST_NUMBER" -eq 1 ]; then
        printf "creating context for %s\n" "$HOST"
        printf "with API_KEY: %s\n" "${X_API_KEY}"
        quick context create --host "${HOST}" --key "${X_API_KEY}"
    fi
}

@test "should deploy product-gateway" {
    run quick gateway create ${GATEWAY}
    echo "$output"
    sleep 30
    [ "$status" -eq 0 ]
    [ "$output" = "Create gateway ${GATEWAY} (this may take a few seconds)" ]
}

@test "should apply schema to product-gateway" {
    run quick gateway apply ${GATEWAY} -f schema.gql
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Applied schema to gateway ${GATEWAY}" ]
}

@test "should create product-topic" {
    run quick topic create "${PRODUCT_TOPIC}" --key-type int --value-type schema --schema "${GATEWAY}.${PRODUCT_TYPE}"
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${PRODUCT_TOPIC}" ]
}

@test "should create purchase-topic" {
    run quick topic create "${PURCHASE_TOPIC}" --key-type string --value-type schema --schema "${GATEWAY}.${PURCHASE_TYPE}"
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${PURCHASE_TOPIC}" ]
}

@test "should ingest data in topics" {
    curl --request POST --url "${PRODUCT_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./products.json"
    sleep 5
    curl --request POST --url "${PURCHASE_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./purchases.json"
    sleep 5
}

@test "should retrieve all item" {
    sleep 30
    result="$(${GRAPHQL_CLI} < query.gql | jq -j .findPurchase)"
    echo "$result"
    expected="$(cat result.json)"
    [ "$result" = "$expected" ]
}

teardown() {
    if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
        quick gateway delete ${GATEWAY}
        quick topic delete ${PRODUCT_TOPIC}
        quick topic delete ${PURCHASE_TOPIC}
    fi
}
