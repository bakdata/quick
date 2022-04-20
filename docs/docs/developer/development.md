# Development

## Tests

We differentiate between unit tests and integration tests in our test suite.
The latter has a custom annotation `@IntegrationTest` that should be used to mark those tests.
We don't run them for draft PRs to keep the feedback loop.

## Build logic

We use custom Gradle build logic to orchestrate our builds.
It's located in the `build-logic` directory and referenced by `includeBuild("build-logic")` in `settings.gradle.kts`.
The build-logic contains two modules: convention and libraries.

### Convention

The convention module configures conventions and plugins that can be applied to a subproject.

`base-dependencies`
:   This convention applies dependencies that are used throughout all Quick subprojects.
    Among others, this includes our logging setup and test dependencies.

`http-service`
:   This adds Micronauts dependencies for running an HTTP service. 
    It allows configuring whether the service is secured.

`BasePlugin`
:   The [BasePlugin](https://github.com/bakdata/quick/blob/master/build-logic/convention/src/main/kotlin/buildlogic/convention/BasePlugin.kt)
    is applied to all Quick projects.
    It configures Java, Compiler Settings, Lombok, and more.

`QuickJibPlugin`
:   The plugin applies Google's Jib plugin and sets the image location.

`QuickCodeQualityPlugin`
:   Plugin that ensures the project's code quality.
    This includes checkstyle, errorprone, nullaway, and jacoco.
    See [Contributing - Code Quality](contributing.md#code-quality) for detailed information.

`ReporterPlugin`
:   The reporter plugin is applied to the root project.
    It allows the aggregation of data from subprojects.
    For example, we use this to collect and merge Jacoco test coverage for the whole project.
    See [Operations - Registry](operations.md#container-registry) for more information.

### Libraries

The libraries module holds all information about all dependencies.
This makes sure they're defined in a common place.

## OpenAPI

The manager API is specified in [the `openapi` directory](https://github.com/bakdata/quick/tree/master/openapi/spec).
We use the [OpenAPI generator](https://github.com/OpenAPITools/openapi-generator) (Version 4.3.1) to generate our
[python client](https://github.com/bakdata/quick-cli/tree/master/quick_client).

```shell
java -jar openapi-generator-cli.jar generate \
-i quick/openapi/spec/Quick-Manager-v1.yaml \
-g python \ 
-o quick-cli/ \
-p=generateSourceCodeOnly=true,packageName=quick_client \
--global-property apiTests=false,modelTests=false,modelDocs=false
```

This assumes the following directory structure:

``` 
quick-project
├── openapi-generator-cli.jar
├── quick
└── quick-cli
```

## Documentation

The documentation is part of the main repository.
It's built with [mkdocs-materialize](https://squidfunk.github.io/mkdocs-material/).
mkdocs generates static websites from markdown files.
You can find all related files in the `docs` directory.

### Local development

mkdocs is a Python project.
You can install all required dependencies with the provided `requirements.txt`.
Run `mkdocs serve` from the `docs` directory for local development.
Then you can view a live version of the documentation on [http://localhost:8000](http://localhost:8000).
mkdocs is configured by the `mkdocs.yaml` file.
The markdown files are in their own `docs` directory because mkdocs expects the files in a subdirectory.

### Build

We deploy our built documentation to the `gh-pages` branch.
For versioning, we use [`mike`](https://github.com/jimporter/mike).
As suggested by `mike`, we omit the documentation version's patch.
To push a new version, run:
```shell
mike deploy x.y latest
```

This command also sets `x.y` as `latest`.
With that, `/latest` redirects to `/x.y`.
If you update an older release, omit `latest`, e.g.:
```shell
mike deploy x.y
```
We also want to redirect `/` to `/latest/`, which redirects to `/x.y/`.
This was done with:
```shell
mike set-default latest
```
It creates an `index.html` in the root, redirecting to `/latest/`.
Unless the file was deleted, there is no need to run the command above.


!!! note
	These commands should only be run locally for testing purposes. 
    Therefore, we drop the `--push` flag here.

### Deployment

As described, `mike` pushes to the `gh-pages` branch.
GitHub automatically hosts the branch on `bakdata.github.io/quick`.
