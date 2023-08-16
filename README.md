# Quick

[![Latest release](https://img.shields.io/github/v/release/bakdata/quick)](https://github.com/bakdata/quick/releases/latest)
[![Build status](https://github.com/bakdata/quick/actions/workflows/master-ci.yml/badge.svg)](https://github.com/bakdata/quick/actions/workflows/master-ci.yml)

> [!WARNING]
> We are currently rethinking Quick's approach and moving from applications to composable libraries.
> During this transition, there will be less updates in this repository.
> Visit our [user guide](https://bakdata.github.io/quick/latest/user/) for more information.

For detailed usage and installation instructions, check out
the [documentation](https://bakdata.github.io/quick/latest/user/).

## Features

- **Query Apache Kafka topics**: Reference topics in your GraphQL schema and Quick will query their content.
- **Build complex schemas**: Combine the data of topics in a single type and let Quick resolve their references.
- **Real-time updates**: Get real-time updates of your data by using GraphQL's subscriptions.
- **Include custom APIs**: Enrich your GraphQL schema with data from your own REST APIs.
- **Data ingest**: You can get data into your Apache Kafka cluster through GraphQL mutations and a REST API.
- **Manage your Apache Kafka environment**: Create new topics and deploy your Kafka Streams applications with a single
  command.

## Documentation

- [What is Quick?](https://bakdata.github.io/quick/latest/user/)
- [Setup Quick and its infrastructure](https://bakdata.github.io/quick/latest/user/getting-started/setup-quick/)
- [Setup Quick CLI](https://bakdata.github.io/quick/latest/user/getting-started/setup-cli)
- [Working with Quick](https://bakdata.github.io/quick/latest/user/getting-started/working-with-quick/)
- [Examples](https://bakdata.github.io/quick/latest/user/examples)

## Contributing

We are happy if you want to contribute to this project.
If you find any bugs or have suggestions for improvements, please open an issue.
We are also happy to accept your PRs.
Just open an issue beforehand and let us know what you want to do and why.

See our [contribution guide](https://bakdata.github.io/quick/latest/developer/contributing/)
and [development overview](https://bakdata.github.io/quick/latest/developer/development/) for more information.

## License

Quick is licensed under the [Apache 2.0 license](https://github.com/bakdata/quick/blob/master/LICENSE).
