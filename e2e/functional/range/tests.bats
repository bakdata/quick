#!/usr/bin/env ./test/libs/bats/bin/bats
# shellcheck shell=bats

CONTENT_TYPE="content-type:application/json"
API_KEY="X-API-Key:${X_API_KEY}"
TOPIC="user-request-range"
TYPE="UserRequests"
GATEWAY="range-gateway-test"
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

@test "should deploy product-gateway" {
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

#TODO: Remove skip after implementing the Mirror and query service
@test "should create user-request-range topic with key integer and value schema" {
    skip
    run quick topic create ${TOPIC} --key-type integer --value-type schema --schema "${GATEWAY}.${TYPE}" --range-field timestamp --point
    echo "$output"
    [ "$status" -eq 0 ]
    [ "$output" = "Created new topic ${TOPIC}" ]
}

@test "should ingest valid data in user-request-range" {
    skip
    curl --request POST --url "${INGEST_URL}" --header "${CONTENT_TYPE}" --header "${API_KEY}" --data "@./user-requests.json"
}

@test "should retrieve range of inserted items" {
    skip
    sleep 30
    result="$(${GRAPHQL_CLI} < query-single.gql | jq -j .userRequests)"
    expected="$(cat result-single.json)"
    echo "$result"
    [ "$result" = "$expected" ]
}

teardown() {
    if [[ "${#BATS_TEST_NAMES[@]}" -eq "$BATS_TEST_NUMBER" ]]; then
        quick gateway delete ${GATEWAY}
        #TODO: Uncomment the topic deletion after the Range Mirror implementation is ready
        # quick topic delete ${TOPIC}
    fi
}
