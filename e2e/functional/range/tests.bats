#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
PRODUCT_TYPE="Product"
PRODUCT_PRICE_TOPIC="product-price-range-test"
GATEWAY="range-gateway-test"
PRODUCT_PRICE_INGEST_URL="${HOST}/ingest/${PRODUCT_PRICE_TOPIC}"
GRAPHQL_URL="${HOST}/gateway/${GATEWAY}/graphql"
GRAPHQL_CLI="gql-cli ${GRAPHQL_URL} -H ${API_KEY}"


setup() {
    if [ "$BATS_TEST_NUMBER" -eq 1 ]; then
        printf "creating context for %s\n" "$HOST"
        printf "with API_KEY: %s\n" "${X_API_KEY}"
        quick context create --host "${HOST}" --key "${X_API_KEY}"
    fi
}

@test "should deploy range-gateway" {
    run quick gateway create ${GATEWAY}
    echo "$output"
    sleep 30
    [ "$status" -eq 0 ]
    [ "$output" = "Create gateway ${GATEWAY} (this may take a few seconds)" ]
}

@test "should apply schema to range-gateway" {
    run quick gateway apply ${GATEWAY} -f schema.gql
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Applied schema to gateway ${GATEWAY}" ]
}

@test "should create product-price-range topic" {
    run quick topic create ${PRODUCT_PRICE_TOPIC} --key-type int --value-type schema --schema "${GATEWAY}.${PRODUCT_TYPE}" --range-field timestamp
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${PRODUCT_PRICE_TOPIC}" ]
}

@test "should ingest valid data in product-price-range" {
    curl --request POST --url "${PRODUCT_PRICE_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./products.json"
}

@test "should retrieve range of inserted items" {
    sleep 30
    result="$(${GRAPHQL_CLI} < query-range.gql | jq -j .productPriceInTime)"
    expected="$(cat result-range.json)"
    echo "$result"
    [ "$result" = "$expected" ]
}

teardown() {
    if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
        quick gateway delete ${GATEWAY}
        quick topic delete ${PRODUCT_PRICE_TOPIC}
    fi
}
