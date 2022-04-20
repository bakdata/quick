#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
TOPIC="crud-product-topic-test"
GATEWAY="crud-product-gateway-test"
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

@test "should create product-topic with key long and value string" {
    run quick topic create ${TOPIC} --key-type long --value-type string
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${TOPIC}" ]
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

@test "should ingest in product-topic" {
    curl --request POST --url "${INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data '{"key": 123, "value": "T-Shirt"}'
}

@test "should retrieve inserted item" {
    result="$(${GRAPHQL_CLI} < query.gql | jq -j .findProduct)"
    [ "$result" = "T-Shirt" ]
}

@test "should ingest multiple product in product-topic" {
    curl --request POST --url "${INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data '[ {"key": 123, "value": "T-Shirt (black)"}, {"key": 456, "value": "Jeans"}, {"key": 789, "value": "Shoes"}]'
}

@test "should retrieve updated item" {
    result="$(${GRAPHQL_CLI} < query.gql | jq -j .findProduct)"
    [ "$result" = "T-Shirt (black)" ]
}

@test "should retrieve new item" {
    result="$(${GRAPHQL_CLI} < query-new-item.gql | jq -j .findProduct)"
    [ "$result" = "Jeans" ]
}

@test "should delete key-value pair in product-topic" {
    KEY="123"
    DELETE_RECORD_URL="${INGEST_URL}/${KEY}"
    echo "${DELETE_RECORD_URL}"
    curl --request DELETE --url "${DELETE_RECORD_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}"
    echo "$output"
}

@test "should not retrieve deleted item" {
    result="$(${GRAPHQL_CLI} < query.gql | jq -j .findProduct)"
    echo "$result"
    [[ "$result" =~ "null" ]]
}

teardown() {
    if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
        quick gateway delete ${GATEWAY}
        quick topic delete ${TOPIC}
    fi
}
