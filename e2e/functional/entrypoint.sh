#!/bin/sh

# This entrypoint goes inside of the tests directory and finds all the e2e subfolder tests (crud, schema, etc.)
# Then for each folder it adds the API_KEY and the HOST and executes the bats command

cd /tests || exit

COMMAND="cd '{}' && printf '\n ★ Running tests in: ' && pwd && X_API_KEY=${X_API_KEY} HOST=${HOST} bats *.bats && printf '\n'"
find . -maxdepth 1 -type d \( ! -name . \) -exec bash -c "$COMMAND" \;
