# E2E test
There are three scenarios tested: </br>
1. CRUD
2. Schema
3. Multi-stream

**NOTE:** Subscription tests are currently skipped by bats. (WIP)

## Prerequisite
You can specify the quick-cli version and the repository of distributions (package index)
via providing the `QUICK_CLI_VERSION` and `INDEX` arguments, respectively. 
Supported repositories are [PyPI](https://pypi.org/project/quick-cli/)
and [Test PyPI](https://test.pypi.org/project/quick-cli/).
To build an image using `Test PyPI`,
you must pass `test` value to the `INDEX` argument.
Providing any other argument results in using `PyPI`.
If you don't give any argument explicitly,
its default value is used to build an image. 
The default values are `QUICK_CLI_VERSION=0.7.0` and `INDEX=main`.

Example for building the image with the stable version of quick-cli
```bash
docker build --build-arg QUICK_CLI_VERSION=<Version> -t quick-e2e-test-runner:<TAG> .
```
Example for building the image with the dev version of quick-cli
```
docker build -t quick-e2e-test-runner --build-arg INDEX=test --build-arg QUICK_CLI_VERSION=0.7.0.dev6  .
```


## How to run e2e tests
Just run the e2e tests use the following command in the current directory:
```bash
docker run -v $(pwd):/tests -e X_API_KEY=$QUICK_API_KEY -e HOST=$QUICK_HOST quick-e2e-test-runner --rm -it 
```
The container will iterate over the folders and execute the `.bats` file inside them. You can then see the execution result on the console. For more information refer to the `entrypoint.sh` file.
