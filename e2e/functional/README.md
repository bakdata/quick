# E2E test
There are three scenarios tested: </br>
1. CRUD
2. Schema
3. Multi-stream

**NOTE:** Subscription tests are currently skipped by bats. (WIP)

## Prerequisite
You can use the [justfile](../../justfile) to build the E2E test runner.

Example for building the image with the stable version of quick-cli:
```bash
just e2e-build-runner <QUICK_CLI_VERSION>
```
You can find the stable quick-cli version in the [PyPI](https://pypi.org/project/quick-cli/).

Example for building the image with the dev version of quick-cli:
```bash
just e2e-build-runner-dev <QUICK_CLI_DEV_VERSION>
```
You can find the dev quick-cli version in the [Test PyPI](https://test.pypi.org/project/quick-cli/).

## How to run e2e tests
Run all the e2e tests use the following command in the current directory:
```bash
just e2e-run-all <X_API_KEY> <QUICK_HOST>
```
Alternatively, you can run each test. For example:
```bash
just e2e-run-crud <X_API_KEY> <QUICK_HOST>
```
or
```bash
just e2e-run-range <X_API_KEY> <QUICK_HOST>
```

For help just run:
```bash
just
```