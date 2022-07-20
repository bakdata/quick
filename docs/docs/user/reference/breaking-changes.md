# Breaking changes

## 0.7

### Avro configuration

- The configuration `QUICK_AVRO_NAMESPACE` is now called `QUICK_SCHEMA_AVRO_NAMESPACE`.
- `avro.namespace` was removed from the Helm chart. Instead, use `QUICK_SCHEMA_AVRO_NAMESPACE` in `quickConfig`.
