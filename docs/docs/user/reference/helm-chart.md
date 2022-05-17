# Quick Helm chart

Below you can find the default `value.yaml` of Quick's Helm chart.
```yaml
image:
  # The base repository of the images
  repository: bakdata
  # The version of quick to deploy
  tag: "{{ quick_version }}"
  # The image pull policy of manager and ingest service
  pullPolicy: "Always"

avro:
  # The namespace used for created avro namespaces
  # see https://avro.apache.org/docs/current/spec.html
  namespace: ""

# These configurations apply to both the helm chart ingresses and the gateway ingresses
ingress:
  # Whether the ingress uses ssl
  ssl: true
  # Which entrypoint Traefik should use
  # This must match the ssl configuration: By default, websecure means ssl=true and web ssl=false
  entrypoint: "websecure"
  # Host of the ingress
  # This must be set when using a domain and can be empty when using an ip address
  host: ""

# Configuration for the manager deployment
manager:
  name: "quick-manager"
  replicaCount: 1
  podAnnotations: {}

# Configuration for the ingest service deployment
ingest:
  # This value is also set as an enviornment variable for quick services so that they know how to connect to this service
  name: "quick-ingest"
  replicaCount: 1
  podAnnotations: {}

# The logging config quick should use, mainly for debugging purposes
# If empty, loglevel is set to info
# see https://logging.apache.org/log4j/2.x/manual/configuration.html#YAML
log4jConfig: {}

# The api key securing all APIs
# This should be set through the CLI
apiKey: ""

# Environment variables configuring all services of quick
# see the reference for more detailed information
quickConfig:
  QUICK_KAFKA_BOOTSTRAP_SERVER: quick-kafka.default.svc.cluster.local:9092
  QUICK_KAFKA_SCHEMA_REGISTRY_URL: http://quick-sr-schema-registry.default.svc.cluster.local:8081
  QUICK_KAFKA_INTERNAL_PARTITIONS: "3"
  QUICK_KAFKA_INTERNAL_REPLICATION_FACTOR: "1"
  QUICK_TOPIC_REGISTRY_SERVICE_NAME: internal-topic-registry
  QUICK_TOPIC_REGISTRY_TOPIC_NAME: __topic-registry
  QUICK_TOPIC_REGISTRY_PARTITIONS: "3"
  QUICK_TOPIC_REGISTRY_REPLICATION_FACTOR: "1"
  QUICK_SCHEMA_FORMAT: "Avro"
```
