# Quick configuration

## Kafka

| Environment Variable                      | Required         | Description                                      |
|-------------------------------------------|------------------|--------------------------------------------------|
| `QUICK_KAFKA_BOOTSTRAP_SERVER`            | :material-check: | Kafka address to connect to                      |
| `QUICK_KAFKA_SCHEMA_REGISTRY_URL`         | :material-check: | Schema Registry URL to connect to                |
| `QUICK_KAFKA_APPLICATION_ID`              | :material-close: | Application id to use                            |
| `QUICK_KAFKA_INTERNAL_PARITITIONS`        | :material-check: | Number of partitions new topics are created with |
| `QUICK_KAFKA_INTERNAL_REPLICATION_FACTOR` | :material-check: | Replication factor of Kafka topics               |

## Mirror

| Environment Variable  | Required         | Description                                             |
|-----------------------|------------------|---------------------------------------------------------|
| `QUICK_MIRROR_PREFIX` | :material-close: | Prefix of Kubernetes deployments for mirror deployments |


## Schema

| Environment Variable            | Required                                 | Description                                                            |
|---------------------------------|------------------------------------------|------------------------------------------------------------------------|
| `QUICK_SCHEMA_FORMAT`           | :material-close:                         | Schema format Quick should use. Valid values: Avro (default), Protobuf |
| `QUICK_SCHEMA_AVRO_NAMESPACE`   | :material-check: (if format is Avro)     | Namespace for Avro schemas generated by Quick from GraphQL             |
| `QUICK_SCHEMA_PROTOBUF_PACKAGE` | :material-check: (if format is Protobuf) | Package name for Protobuf schemas generated by Quick from GraphQL      |

## Topic Registry

| Environment Variable                      | Required         | Description                                                |
|-------------------------------------------|------------------|------------------------------------------------------------|
| `QUICK_TOPIC_REGISTRY_TOPIC_NAME`         | :material-check: | Topic backing the topic registry                           |
| `QUICK_TOPIC_REGISTRY_SERVICE_NAME`       | :material-check: | Service name of the topic registry                         |
| `QUICK_TOPIC_REGISTRY_PARTITIONS`         | :material-check: | Partition count of the topic backing the topic registry    |
| `QUICK_TOPIC_REGISTRY_REPLICATION_FACTOR` | :material-check: | Replication factor of the topic backing the topic registry |


## Deployment

| Environment Variable                        | Required         | Description                                                                                                    |
|---------------------------------------------|------------------|----------------------------------------------------------------------------------------------------------------|
| `QUICK_DOCKER_REGISTRY`                     | :material-check: | Docker registry for use Quick images                                                                           |
| `QUICK_DEFAULT_IMAGE_TAG`                   | :material-check: | Default image tag of Quick images to deploy                                                                    |
| `QUICK_DEFAULT_REPLICAS`                    | :material-check: | Default amount of replicas for Quick deployments                                                               |
| `QUICK_INGRESS_HOST`                        | :material-close: | Host for Kubernetes Ingress objects for gateways                                                               |
| `QUICK_INGRESS_SSL`                         | :material-check: | Flag indicating whether the ingress should use SSL                                                             |
| `QUICK_INGRESS_ENTRYPOINT`                  | :material-check: | Traefik's entrypoint for ingress                                                                               |
| `QUICK_MANAGER_UPDATE_MANAGED_IMAGES`       | :material-check: | Flag indicating whether the manager should ensure deployments have the same image tag                          |
| `QUICK_MANAGER_CREATE_TOPIC_REGISTRY`       | :material-check: | Flag if manager should deploy a topic registry                                                                 |

## Applications Specification

| Environment Variable                               | Required         | Description                                                                                                  |
|----------------------------------------------------|------------------|--------------------------------------------------------------------------------------------------------------|
| `QUICK_APPLICATIONS_SPEC_IMAGE_PULL_POLICY`        | :material-close: | Image pull policy of the deployed applications by Quick. Valid values: Always (default), IfNotPresent, Never |
| `QUICK_APPLICATIONS_SPEC_RESOURCES_MEMORY_LIMIT`   | :material-close: | Memory limit for deployments                                                                                 |
| `QUICK_APPLICATIONS_SPEC_RESOURCES_MEMORY_REQUEST` | :material-close: | Memory request for deployments                                                                               |
| `QUICK_APPLICATIONS_SPEC_RESOURCES_CPU_LIMIT`      | :material-close: | Cpu limit for deployments                                                                                    |
| `QUICK_APPLICATIONS_SPEC_RESOURCES_CPU_REQUEST`    | :material-close: | Cpu requests for deployments                                                                                 |

## Gateway

| Environment Variable | Required         | Description                               |
|----------------------|------------------|-------------------------------------------|
| `QUICK_SCHEMA_PATH`  | :material-check: | The path where the schema file is located |
