"""
Python functions for defining macros in mkdocs
See https://mkdocs-macros-plugin.readthedocs.io/en/latest/
"""


def define_env(env):
    @env.macro
    def quick_cli_command():
        """
        Function for setting the pip install command for quick cli.
        For dev versions, this uses testpypi and installs the latest version.
        For other versions, it install the corresponding quick cli.
        """
        quick_version = env.variables["quick_version"]

        if quick_version.endswith("-dev"):
            return """--index-url https://test.pypi.org/simple/ \\
     --extra-index-url https://pypi.org/simple/ \\
     quick-cli"""
        else:
            return f'quick-cli=="{quick_version}"'
