# Roadmap

The roadmap outlines topics that are currently worked on and gives an overview of the project's future direction.
We track and priorities planned features through the corresponding [milestones](https://github.com/bakdata/quick/milestones)
and [project boards](https://github.com/bakdata/quick/projects).

## Upcoming releases

### 0.9

Development: Q4 2022

* Providing DateTime support
* Addressing limitations and extending range queries, which means
giving users a possibility to:
    * make a range exclusively on values 
    * make a range exclusively on keys
    * make a range query with a composite key
    * make a range using DateTime
* Simplifying the `@topic` directive - using additional 
directives with smaller responsibilities instead
* Redefining topic semantics
* Improving Gateway Performance (using JSON instead of generic types)


### Further ideas

* A possibility to deploy a mirror without creating a topic
* Extending `Mutation` possibilities, for example, to ingest an array of values
* Providing a `between` semantics for having a range open on two fields


## Completed releases

The [changelog](../changelog) has a detailed list of releases.

### 0.8

* Range queries support
* Improved gateway performance: Pre-computation of a key's location
* Kafka 3.0 support

### 0.7

* Protobuf support

### 0.6

* Open-Source release 🎉
