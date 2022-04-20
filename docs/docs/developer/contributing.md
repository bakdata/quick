# Contributing

## Code quality

The project uses several plugins to ensure code quality.
Most of them run during the compilation of the project.
Any warnings or errors should be addressed before merging.

### Checkstyle

We use [Checkstyle](https://checkstyle.sourceforge.io/) to enforce our coding style.
The configuration is located in the `config/checkstyle/checkstyle.xml`.
It's based on Google's Java coding style.
You can download the Checkstyle Plugin in the IntelliJ Plugin Store.
By importing the `checkstyle.xml` as code style, the reformat action ++ctrl+alt+l++ works with it.
Further, you can run Gradle tasks `checkstyleMain` and `checkstyleTest`.
These tasks list problems in the shell and create an HTML report located in `build/reports/checkstyle/`.

### Copyright header

All *.java and *.kt files should contain a copyright header.
IntelliJ can automatically prepend the header for new classes using a copyright profile.
To set up the functionality, import `config/copyright/quick-license.xml`
in `Settings | Editor | Copyright | CopyrightProfiles`.
Checkstyle will alert you if the header is missing.

To prevent Intellij from putting headers to all other kinds of files,
you can create a custom shared scope in `Settings | Appearance & Behaviour | Scopes` with pattern `file:*.java||file:*.kt`
and associate this with the copyright profile in `Settings | Editor | Copyright`.

### Error Prone

[Error Prone](https://errorprone.info/) extends the Java Compiler to catch common mistakes.
It's enabled by default when compiling with Gradle.

!!! attention
    Lombok and Error Prone don't work that well together.
    There might be some false-positives when Lombok is used.
    If possible, use `@SuppressWarning`.
    Otherwise, disable the check completely in the  [`QuickCodeQualityPlugin`](development.md#convention).
    Furthermore, Error Prone throws an `IndexOutOfBounds` exception for the pattern `UnusedVariable` when a Lombok annotation adds an unused field.
    That may usually happen with `@Sl4j`.
    In such cases, remove the annotation as it isn't used.

#### IntelliJ Setup

To add the plugin, start the IDE and find the Plugins dialog.
Browse Repositories, choose `Category: Build`, and find the Error-prone plugin.
Right-click and choose `Download and install`.
The IDE will restart after you’ve exited these dialogs.

To enable Error Prone, choose `Settings | Compiler | Java Compiler | Use compiler: Javac with error-prone`
and also make sure `Settings | Compiler | Use external build` is **not** selected.

### NullAway

[NullAway](https://github.com/uber/NullAway) is a tool to help eliminate NullPointerExceptions (NPEs) in your Java code.
It's built as a plugin to Error Prone.
With NullAway, the compiler assumes that a variable is never null unless there is a `@Nullable` annotation.

!!! attention
    `@Nullable` has its origin in the JSR 305 that has been dormant since 2012.
    Therefore, using  `javax.annotation.Nullable` is problematic.
    We chose to use `edu.umd.cs.findbugs.annotations.NonNull` instead.


### Jacoco

We use Jacoco for test coverage.
The [`ReporterPlugin`](development.md#convention) creates the task `codeCoverageReport`
that creates a test report in `build/reports/jacoco`.
There is an HTML report for manual inspection and an XML report that you can export to Sonarqube.

### Sonarqube

There is currently no public Sonarqube project.
You can run one locally and set the properties in `gradle.properties`.

## Issues

We use GitHub issues to keep track of our tasks.
Issues should always have one or more [labels](https://github.com/bakdata/quick/labels).

## Pull Request Workflow

The `master` branch is protected, i.e., you can't push directly to it.
We use a workflow similar to [GitHub Flow](https://guides.github.com/introduction/flow),
except that we squash Pull Requests (PRs).
You start by creating a new local branch, committing the changes, and creating a PR on GitHub.
Please don't wait till the end of creating the PR and try to keep it small(ish).

### Git Branch

The branch should have a meaningful name and roughly follow the scheme `TYPE`/`COMPONENT`/`TOPIC`.
For example, when working on a new feature in the manager, `feature/mananger/new-feature` could be used.
You can drop `TYPE` and/or `COMPONENT` if they aren't applicable.

The name carries some importance because

* it's used in the [CI](operations.md#CI) for tagging the images created for the PR.
  For example, the branch `feature/mananger/new-feature` can be deployed with the tag `feature-manager-new-feature`.

* it helps find your way in git.

### Pull Request

Some general guidelines regarding Pull Requests:

- Use a meaningful title (not the default branch name) and add a short description for non-trivial changes.

- Publish your branch early on, and create a draft PR in case it isn't ready.
  This allows for early feedback and helps prevent misunderstandings.

- Keep your branch up-to-date with master, otherwise, you can't merge it.

- If applicable, link the issues closed by this PR.

- Add a meaningful commit message when merging the PR.
  For example, you can use the PR description.

- Be careful with dependent branches/PRs.
  Otherwise, reviewers might have a hard time getting through the diff.
  See the next section for more information.

### Dependent Pull Requests

Squashing merge commits is great for having a clean commit history in master.
It's sometimes, however, troublesome when dealing with dependent features.
The StackOverflow answer [Hold on, skip merging](https://softwareengineering.stackexchange.com/questions/351727/working-on-a-branch-with-a-dependence-on-another-branch-that-is-being-reviewed/351790#351790)
explains the preferred workflow in such situations.

## Release process

We follow SemVer versioning.
A new release can be created by manually triggering the GitHub Action workflow [`Create release`](https://github.com/bakdata/quick/actions/workflows/release.yml).
You can specify the scope of the release, i.e., major, minor, or patch.

The process:

- creates a release commit
- creates a git tag
- adds a [changelog](#changelog) entry
- creates a GitHub Release with the changelog entry
- pushes a new Docker image
- adds the corresponding Helm chart to the GitHub release
- adds the reference to the new Helm chart to the Helm Repository
- for minor and major releases, creates a new version in the documentation
- creates a commit with the new dev version

### Changelog

We use the [github-changelog-generator](https://github.com/github-changelog-generator/github-changelog-generator) to create our changelog automatically.
You can find the configuration in the [release workflow](https://github.com/bakdata/quick/blob/master/.github/workflows/release.yml).
We only include closed issues but not pull requests.
If a closed issue shouldn't be included, label it as invalid.
