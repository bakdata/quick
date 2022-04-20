# CLI

Our [CLI](https://github.com/bakdata/quick-cli) is written in Python.
It has two modules: `quick_client` and `quick`.
The `quick_client` module is the REST API client for the manager.
It's automatically generated as explained in [Development - OpenAPI](development.md#openapi)
The `quick` module is a CLI wrapper for the REST API client.

## Development
### Setup

We use [poetry](https://github.com/python-poetry/poetry) as build system.
You can run `poetry install` in the project directory.
This command creates a new virtual environment with all runtime and dev dependencies installed by default.
!!! note
    You can run `poetry config virtualenvs.path <PATH>` to set the path where virtualenvs are created.

To create a new version and install it, run:

```shell
poetry build -f wheel && \
pip install ./dist/quick_cli-0.1.3-py3-none-any.whl --force-reinstall
```

If you want to add a new dependency, make sure to commit the updated `poetry.lock` file.

### Code quality

We use flake8, isort, and black for formatting and linting the project.
You can use the provided pre-commit-config to make sure your PRs pass.
Install the hooks with `pre-commit install` (making them run before each commit).
You can also run them manually with `pre-commit run --all-files`.
Currently, mypy is excluded from pre-commit.
However, feel free to run `mypy .` and fix possible errors.


### Documentation

The CLI comes with an argparse-to-markdown script for generating the CLI's documentation.
The created file is `commands.md`.
After updating commands, update it with the following command:
```shell
python quick/generate_docs.py > commands.md
```

## Architecture

The CLI uses Python's [ArgParse](https://docs.python.org/3/library/argparse.html).
It follows an OOP approach for defining new subcommands.
There are two base classes:

`Group`
:   A Group is a subcommand that has one more child subcommands.
    The subcommands are defined in its `sub_parser` field.
    !!! attention
        A new top-level group (i.e., used as `quick <new-group>`) must be added the `COMMANDS` list in 
        `commands/__init__.py`.

`Command`
:   A Command is a subcommand that executes an action.
    You can override the `add_args` method to add new required and optional arguments.
    The `execute` method contains the logic for the command action.

This approach using base classes lets us define common behavior,
like recurring arguments (for example, `--debug`) and setup configurations.
