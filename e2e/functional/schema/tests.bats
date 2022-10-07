#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
GATEWAY="schema-product-gateway-test"
TYPE="Product"
TOPIC="schema-product-topic-test"
INGEST_URL="${HOST}/ingest/${TOPIC}"
GRAPHQL_URL="${HOST}/gateway/${GATEWAY}/graphql"
GRAPHQL_CLI="gql-cli ${GRAPHQL_URL} -H ${API_KEY}"


setup() {
    if [ "$BATS_TEST_NUMBER" -eq 1 ]; then
        printf "creating context for %s\n" "$HOST"
        printf "with API_KEY: %s\n" "${X_API_KEY}"
        quick context create --host "${HOST}" --key "${X_API_KEY}"
    fi
}

# @test "should deploy product-gateway with a given schema" {
#     run quick gateway create -s schema-query-single.gql ${GATEWAY}
#     echo "$output"
#     sleep 30
#     [ "$status" -eq 0 ]
#     [ "$output" = "Create gateway ${GATEWAY} (this may take a few seconds)" ]
# }

@test "should create product-topic with schema" {
    run quick topic create ${TOPIC} --key-type int --value-type schema --schema "${GATEWAY}.${TYPE}"
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${TOPIC}" ]
}

# @test "should ingest valid data in product-topic" {
#     curl --request POST --url "${INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./products.json"
# }

# @test "should not ingest invalid data in product-topic" {
#     run curl --request POST --url "${INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./invalid-product.json"
#     echo "$output"
#     [[ "$output" =~ "Error" ]]
# }

# @test "should retrieve inserted item" {
#     sleep 30
#     result="$(${GRAPHQL_CLI} < query-single.gql | jq -j .findProduct)"
#     expected="$(cat result-single.json)"
#     echo "$result"
#     [ "$result" = "$expected" ]
# }

# @test "should apply new schema to product-gateway" {
#     run quick gateway apply ${GATEWAY} -f schema-query-all.gql
#     [ "$status" -eq 0 ]
#     [ "$output" = "Applied schema to gateway ${GATEWAY}" ]
# }

# @test "should query list of items and retrieve list" {
#     sleep 10
#     result="$(${GRAPHQL_CLI} < query-list.gql | jq -j .findProducts)"
#     echo "$result"
#     expected="$(cat result-list.json)"
#     [ "$result" = "$expected" ]
# }

# @test "should retrieve all item" {
#     result="$(${GRAPHQL_CLI} < query-all.gql | jq -j .allProducts)"
#     echo "$result"
#     expected="$(cat result-all.json)"
#     [ "$result" = "$expected" ]
# }

# teardown() {
#     if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
#         quick gateway delete ${GATEWAY}
#         quick topic delete ${TOPIC}
#     fi
# }
