# E2E test
There are three scenarios tested: </br>
1. CRUD
2. Schema
3. Multi-stream

**NOTE:** Subscription tests are currently skipped by bats. (WIP)

## Prerequisite
You can specify the quick-cli version when you are building the image through the argument `QUICK_CLI_VERSION`. The default version is `0.7.0` 

```bash
docker build --build-arg QUICK_CLI_VERSION=<Version> -t quick-e2e-test-runner:<TAG> .
```

## How to run e2e tests
Just run the e2e tests use the following command in the current directory:
```bash
docker run -v $(pwd):/tests -e X_API_KEY=$QUICK_API_KEY -e HOST=$QUICK_HOST quick-e2e-test-runner --rm -it 
```
The container will iterate over the folders and execute the `.bats` file inside them. You can then see the execution result on the console. For more information refer to the `entrypoint.sh` file.
