#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
USER_RATING_TYPE="UserRating"
PURCHASE_TYPE="Purchase"
USER_RATING_TOPIC="user-rating-range-test"
PURCHASE_TOPIC="purchase-topic-test"
GATEWAY="range-gateway-test"
USER_RATING_INGEST_URL="${HOST}/ingest/${USER_RATING_TOPIC}"
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

@test "should create purchase-topic" {
    run quick topic create "${PURCHASE_TOPIC}" --key-type string --value-type schema --schema "${GATEWAY}.${PURCHASE_TYPE}"
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${PURCHASE_TOPIC}" ]
}

@test "should create user-request-range topic with key integer and value schema" {
    run quick topic create ${USER_RATING_TOPIC} --key-type int --value-type schema --schema "${GATEWAY}.${USER_RATING_TYPE}" --range-field rating
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${USER_RATING_TOPIC}" ]
}

@test "should ingest valid data in user-request-range" {
    curl --request POST --url "${USER_RATING_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./user-ratings.json"
    sleep 5
    curl --request POST --url "${PURCHASE_INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./purchases.json"
    sleep 5
}

@test "should retrieve range of inserted items" {
    sleep 30
    result="$(${GRAPHQL_CLI} < query-range.gql | jq -j .userRatings)"
    expected="$(cat result-range.json)"
    echo "$result"
    [ "$result" = "$expected" ]
}

teardown() {
    if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
        quick gateway delete ${GATEWAY}
        quick topic delete ${USER_RATING_TOPIC}
        quick topic delete ${PURCHASE_TOPIC}
    fi
}
