# Setup Quick CLI

The main tool for administrating Quick is its [CLI](https://github.com/bakdata/quick-cli).
Before you can start to work with Quick, you will have to set it up.

## Installation

The first step is to install the Quick CLI. 
You can do this via pip.
Quick CLI works with Python versions 3.7-3.9:
```shell
pip install {{ quick_cli_command() }}
```

The command `quick -v` lets you verify that the installation was successful.

## Context configuration

Next, you can configure the Quick cluster's host and API key.
The CLI's [`context`](../reference/cli-commands.md#quick-context) command manages this configuration.
To create a new context named `guide`, you can run:

```shell
quick context create \
    --host "$QUICK_URL" \
    --key "$QUICK_API_KEY" \
    --context guide
```

You can then activate the context with the following command:

```shell
quick context activate guide
```

