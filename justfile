set shell := ["bash", "-uc"]

base-directory := justfile_directory()

_default:
    @just --list --unsorted

###############################################################################
## Version
###############################################################################

version := `git tag --sort=v:refname | tail -1`
semver := base-directory + "/.github/semver"

# Print version of the latest release
print-version:
    @echo {{ version }}

# Print current development version
print-dev-version:
	@echo "$({{ semver }} bump minor {{ version }})-dev"

bump-version scope="patch":
	@echo "$({{ semver }} bump {{ scope }} {{ version }})"

###############################################################################
## Documentation
###############################################################################

docs-branch := "gh-pages"
docs-version := `tag=$(git tag --sort=v:refname | tail -1);echo ${tag%.*}`
docs-config := base-directory + "/docs/mkdocs.yml"

# Serve current docs located in ./docs/docs
serve-docs port="8000":
    mkdocs serve --config-file {{ docs-config }} --dev-addr localhost:{{ port }}

# Serve docs deployed in branch 'documentation'
serve-deployed-docs port="8000":
    mike serve --config-file {{ docs-config }} --dev-addr localhost:{{ port }}

# Print the current version of the documentation
print-docs-version:
    @echo {{ docs-version }}

# Print current dev version of the documentation
print-docs-dev-version:
	#!/usr/bin/env bash
	new_version=$({{base-directory }}/.github/semver bump minor {{ version }})
	docs_version=$(echo ${new_version%.*})
	echo "$docs_version-dev"

docs-set-quick-version new-version:
	sed -i "s/quick_version:.*$/quick_version: {{ new-version }}/" {{ docs-config }}

###############################################################################
## Docker Push
###############################################################################

gradle-bin := base-directory + "/gradlew"

# Print the image tag for the current branch
get-branch-image:
    #!/usr/bin/env bash
    ref=$(git branch --show-current)
    # Substitute '/' with '-'
    echo "${ref////-}"

# Push image for current branch for <project>
push-image project:
    #!/usr/bin/env bash
    version=$(just get-branch-image)
    if [ "$version" == "master" ]; then
        echo "Don't push from master"
        exit 1
    fi
    {{ gradle-bin }} {{ project }}:jib -Pversion=$version

# Push all images for current branch
push-images:
    #!/usr/bin/env bash
    version=$(just get-branch-image)
    if [ "$version" == "master" ]; then
        echo "Don't push from master"
        exit 1
    fi
    {{ gradle-bin }} jib -Pversion=$version

###############################################################################
## Helm chart
###############################################################################

chart-dir := base-directory + "/deployment/helm/quick"
chart-target := base-directory + "/deployment/charts"

# Create .tgz of helm chart
package-chart target=chart-target:
    helm package {{ chart-dir }} -d {{ target }}

# Update the helm chart version (default latest release)
set-chart-version chart-version=version:
	@echo "Update Helm chart to {{ chart-version }}"
	sed -i "s/^version:.*$/version: {{ chart-version }}/" {{ chart-dir }}/Chart.yaml
	sed -i "s/^appVersion:.*$/appVersion: {{ chart-version }}/" {{ chart-dir }}/Chart.yaml
