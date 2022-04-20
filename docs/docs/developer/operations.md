# Operations

## Tools

A list of tools we use:

* [Gradle](https://gradle.org/)
* [kubectl](https://kubernetes.io/docs/reference/kubectl/overview/)
  (recommendation: [k9s](https://github.com/derailed/k9s))
* [Helm](https://helm.sh/)
* [Poetry](https://python-poetry.org/)

## Kubernetes Cluster

To deploy Quick, you need access to a Kubernetes cluster.
Each Quick deployment should have its namespace.
However, the infrastructure deployments (Kafka, Schema Registry and Traefik) can be shared between instances.

## Container Registry

All our images are located in our [Docker Hub Registry](https://hub.docker.com/u/bakdata/):

* [quick-mirror](https://hub.docker.com/r/bakdata/quick-mirror)
* [quick-manager](https://hub.docker.com/r/bakdata/quick-manager)
* [quick-gateway](https://hub.docker.com/r/bakdata/quick-gateway)
* [quick-ingest](https://hub.docker.com/r/bakdata/quick-ingest)

To configure docker inside the CI, use the secrets `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` in a workflow to login:

```
- name: Login to Docker Hub
  uses: docker/login-action@v1
  with: {% raw %}
    username: ${{ inputs.username }}
    password: ${{ inputs.token }} {% endraw %} 
```

Once you have a docker configured, you can tag and push images using jib:

```
./gradlew -Pversion=<image-tag> jib
```

## CI

Our CI runs on top of GitHub Actions.
You can find the workflows in `./.github/workflows`.

The main workflow `ci.yaml` has the following tasks:

- build & test project
- push image to registry
- update Helm chart
- update documentation

For a PR branch, the image tag is the branch name.
For the master branch, the image tag is the latest release version incremented by one patch version and a `-dev` suffix.
For example, if the current version is `0.5.3`, pushes an image with the tag `0.6.0-dev`.

The CI also provides a release workflow.
See [release process](contributing.md#release-process) for more information.

## Helm chart

The deployment of Quick in Kubernetes is done with Helm.
[The chart is part of the main repository](https://github.com/bakdata/quick/tree/master/deployment/helm/quick)
and is hosted on our GitHub pages (https://bakdata.github.io/quick).
