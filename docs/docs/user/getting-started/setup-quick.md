# Setup Quick

In this part, you will set up a Quick cluster. This includes:


- optionally creating a local Kubernetes cluster
- running Apache Kafka and Confluent's Schema Registry
- deploying Quick

## Prerequisites

- [k3d (Version 5.3.0+)](https://k3d.io/v5.3.0) and [Docker (Version >= v20.10.5)](https://www.docker.com/get-started/) or an existing Kubernetes cluster (>= 1.21.0)
- [kubectl (Compatible with server version 1.21.0)](https://kubernetes.io/docs/tasks/tools/)
- [Helm (Version 3.8.0+)](https://helm.sh)

## Setup Kubernetes with k3d

If you don't have access to an existing Kubernetes cluster,
this section will guide you through creating a local cluster.
We recommend the lightweight Kubernetes distribution [k3s](https://k3s.io/) for this.
[k3d](https://k3d.io/) is a wrapper around k3s in Docker that lets you get started fast.
You can install it with k3d's installation script:
```shell
wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/v5.3.0/install.sh | bash
```
For other ways of installing k3d,
you can have a look at their [installation guide](https://k3d.io/v5.3.0/#installation).

!!! Attention
	k3s includes [Traefik](https://traefik.io) as a load balancer.
	If you want to use a different Kubernetes distribution, you might have to install Traefik separately.
	For more information, please refer to the [Traefik deployment](setup-quick.md#traefik-optional) section.

With k3d installed, you can create a new cluster called `quick`:
```shell
k3d cluster create quick
```
!!! Note
	This automatically configures `kubectl` to connect to the local cluster by modifying your `~/.kube/config`.
	In case you manually set the `KUBECONFIG` variable or don't want that k3d modifies your config,
	k3d offers [many other options](https://k3d.io/v5.3.0/usage/kubeconfig/#handling-kubeconfigs).

After the command has run, you can check the cluster status with  `kubectl get pods -n kube-system`.
When all returned elements have a `STATUS` of `Running` or `Completed`,
you can create a connection to the load balancer.
For that, open a new terminal and run:
=== "bash/zsh"
	```shell
	export QUICK_PORT=8000
	kubectl port-forward -n kube-system deployment/traefik $QUICK_PORT:8000
	```

=== "fish"
	```shell
	set QUICK_PORT 8000
	kubectl port-forward -n kube-system deployment/traefik $QUICK_PORT:8000
	```
This terminal session has to remain open to keep the connection alive.
Further, you can choose a different port if it's already used.

In the following, the guide will use the variables `QUICK_HOST` and `QUICK_URL` to refer to the load balancer.
Set them like this:

=== "bash/zsh"
	```shell
	export QUICK_HOST="localhost:$QUICK_PORT"
	export QUICK_URL="http://$QUICK_HOST"
	```

=== "fish"
	```shell
	set QUICK_HOST "localhost:$QUICK_PORT"
	set QUICK_URL "http://$QUICK_HOST"
	```

## Traefik (optional)
k3s uses [Traefik](https://doc.traefik.io/traefik/) as its load balancer.
If you are using k3s as your Kubernetes distribution,
you can go directly to the [Kafka deployment](setup-quick.md#kafka) section.
However, if you use another Kubernetes distribution,
you can use this guide to deploy Traefik to your Kubernetes cluster.

This section provides a step-by-step guide on deploying Traefik to your Kubernetes cluster.
Traefik is the Ingress controller Quick needs for load balancing incoming requests.
We recommend the [official Helm chart](https://github.com/traefik/traefik-helm-chart) to deploy Traefik.


1. Add Traefik Helm repository
   ```shell
   helm repo add traefik https://helm.traefik.io/traefik && helm repo update
   ```

2. Deploy Traefik with Helm
   ```shell
   helm upgrade --install traefik traefik/traefik \
       --namespace infrastructure
   ```

!!! Note
	This guide uses the [default values](https://github.com/traefik/traefik-helm-chart/blob/master/traefik/values.yaml) of Traefik's Helm charts for the deployment.
	For instance, Traefik won't use TLS with this configuration.
	For more information on how you can enable TLS, please refer to the [Traefik documentation](https://doc.traefik.io/traefik/https/tls/).


## Kafka

To deploy Kafka, this guide uses Confluent's Helm Chart.

1. Add Confluent's Helm Chart repository

    ```shell
    helm repo add confluentinc https://confluentinc.github.io/cp-helm-charts/ &&  
    helm repo update
    ```

2. Install Kafka, Zookeeper, and the Schema Registry.
   A single Helm chart installs all three components.
   Below you can find an example for the `--values ./kafka.yaml` file configuring the deployment.
    ```shell
    helm upgrade \
        --install \
        --version 0.6.1 \
        --values ./kafka.yaml \
        --namespace infrastructure \
        --create-namespace \
        --wait \
        k8kafka confluentinc/cp-helm-charts
    ```

??? "Kafka Helm Chart Values (`kafka.yaml`)"
	An example value configuration for Confluent's Helm chart.
	This configuration deploys a single Broker, a Schema Registry and Zookeeper with minimal resources.
	```yaml title="kafka.yaml"
	cp-zookeeper:
	  enabled: true
	  imageTag: 6.1.1
	  servers: 1
	  heapOptions: "-Xms124M -Xmx124M"
	  overrideGroupId: k8kafka
	  fullnameOverride: "k8kafka-cp-zookeeper"
	  resources:
		requests:
		  cpu: 50m
		  memory: 0.2G
		limits:
		  cpu: 250m
		  memory: 0.2G
	  prometheus:
		jmx:
		  enabled: false

	cp-kafka:
	  brokers: 1
	  imageTag: 6.1.1
	  enabled: true
	  podManagementPolicy: Parallel
	  configurationOverrides:
		"auto.create.topics.enable": false
		"offsets.topic.replication.factor": 1
		"transaction.state.log.replication.factor": 1
		"transaction.state.log.min.isr": 1
	  resources:
		requests:
		  cpu: 50m
		  memory: 0.5G
		limits:
		  cpu: 250m
		  memory: 0.5G
	  prometheus:
		jmx:
		  enabled: false
	  persistence:
		enabled: false

	cp-schema-registry:
	  enabled: true
	  imageTag: 6.1.1
	  fullnameOverride: "k8kafka-cp-schema-registry"
	  overrideGroupId: "k8kafka"
	  kafka:
		bootstrapServers: "PLAINTEXT://k8kafka-cp-kafka-headless:9092"
	  resources:
		requests:
		  cpu: 50m
		  memory: 0.25G
		limits:
		  cpu: 250m
		  memory: 0.25G
	  prometheus:
		jmx:
		  enabled: false

	cp-ksql-server:
	  enabled: false
	cp-kafka-connect:
	  enabled: false
	cp-control-center:
	  enabled: false
	cp-kafka-rest:
	  enabled: false
	```

Depending on your system, it can take a couple of minutes before all components are up and running.
You can view the status of the created pods by running `kubectl get pods -n infrastructure`.
You should now have Zookeeper, Kafka, and the Schema Registry running in a namespace called `infrastructure`.

In the Kubernetes cluster,
you can connect to Kafka with `k8kafka-cp-kafka.infrastructure:9092` and the Schema Registry with `http://k8kafka-cp-schema-registry.infrastructure:8081`.
You are now set to deploy Quick itself.

## Quick

Quick comes with its Helm chart for installing it in Kubernetes clusters.

1. Add the Quick Helm chart repository and update the index:
	```shell
	helm repo add quick https://bakdata.github.io/quick && helm repo update
	```
2. Create a random secret key called `QUICK_API_KEY`:

    === "bash/zsh"
        ```shell
        export QUICK_API_KEY="random-key"
        ```

    === "fish"
        ```shell 
		set QUICK_API_KEY "random-key"
	    ```
 
3. Install Quick with Helm.
  Below is an example `quick.yaml` as a configuration for Quick in local clusters.
	```shell
	helm upgrade --install quick quick/quick \
	  --namespace quick \
	  --version {{ quick_version }} \
	  --create-namespace \
	  --set apiKey="$QUICK_API_KEY" \
	  -f "./quick.yaml"
	```

??? "Quick Helm Chart Values (`quick.yaml`)"
	An example configuration for local Kubernetes clusters.
	It lets Quick work with a single Kafka broker and HTTP instead of HTTPS.
	For more information about Quick's Helm Chart configuration, please see [the reference](../reference/helm-chart.md).
	```yaml title="quick.yaml"
	image:
	  pullPolicy: "Always"
	  tag: "{{ quick_version }}"

	ingress:
	  ssl: False
	  entrypoint: "web"

	avro:
	  namespace: "quick"

	manager:
	  name: "quick-manager"
	  replicaCount: 1

	ingest:
	  name: "quick-ingest"
	  replicaCount: 1

	quickConfig:
	  QUICK_DEFAULT_REPLICAS: "1"
	  QUICK_KAFKA_BOOTSTRAP_SERVER: k8kafka-cp-kafka.infrastructure:9092
	  QUICK_KAFKA_INTERNAL_PARTITIONS: "3"
	  QUICK_KAFKA_INTERNAL_REPLICATION_FACTOR: "1"
	  QUICK_KAFKA_SCHEMA_REGISTRY_URL: http://k8kafka-cp-schema-registry.infrastructure:8081
	  QUICK_TOPIC_REGISTRY_PARTITIONS: "3"
	  QUICK_TOPIC_REGISTRY_REPLICATION_FACTOR: "1"
	  QUICK_TOPIC_REGISTRY_SERVICE_NAME: internal-topic-registry
	  QUICK_TOPIC_REGISTRY_TOPIC_NAME: __topic-registry
	```

You can check the status of Quick by running `kubectl get pods -n quick`.
There should be three running pods: `quick-manager`, `quick-ingest`, and `internal-topics-registry`.
