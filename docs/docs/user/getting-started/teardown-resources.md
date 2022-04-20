# Teardown resources

This section progresses from single resources to the underlying infrastructure.
If you want to delete everything, you can skip deleting single resources.

## Quick

- Delete the gateway:
   ```shell
   quick gateway delete example
   ```
- Delete the topics:
   ```shell
   quick topic delete purchase
   quick topic delete product
   ```

- Delete the Helm chart:
  ```shell
  helm delete quick -n quick
  ```
  
- Delete the namespace:
  ```shell
  kubectl delete namespace quick
  ```

## Infrastructure

- Delete the Helm chart:
  ```shell
  helm delete k8kafka -n infrastructure
  ```

- Delete the namespace:
  ```shell
  kubectl delete namespace infrastructure
  ```

## Local Cluster

- Delete the cluster:
  ```shell
  k3d cluster delete quick
  ```
