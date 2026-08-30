# Maintain documentation

This guide defines how maintainers place, verify, and review project
documentation. Markdown under `docs/` is the single source of truth for the
published site.

When preparing a release, follow the
[release documentation checklist](release-readiness.md) before publication.

## Choose the page type

Follow Diataxis and give each page one primary reader need.

| Page type        | Reader need                                                           | Location and examples                                                                                                                                                                                                                          |
|------------------|-----------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tutorial         | Learn through a guided sequence.                                      | `docs/tutorials/`, such as [Getting started](../tutorials/getting-started.md).                                                                                                                                                                 |
| How-to guide     | Complete a specific task or resolve a specific problem.               | `docs/how-to/`, such as [Maven](../how-to/maven.md), [Gradle](../how-to/gradle.md), [CLI](../how-to/cli.md), and [Troubleshooting](../how-to/troubleshooting.md).                                                                              |
| Reference        | Look up exact inputs, outputs, values, defaults, or limitations.      | `docs/reference/`, including [Supported capabilities](../reference/supported-capabilities.md), [Generator configuration](../reference/generator-configuration.md), and [Generated output examples](../reference/generated-output-examples.md). |
| Explanation      | Understand purpose, boundaries, or why the system behaves as it does. | `docs/explanation/`, such as [Project purpose and scope](../explanation/purpose-and-scope.md) and [How the generation pipeline works](../explanation/architecture.md).                                                                         |
| Maintainer guide | Change or reason about the implementation and project process.        | Existing implementation architecture and `docs/contributing/`, grouped under **Maintainer documentation** in `mkdocs.yml`.                                                                                                                     |

Split content when one page would otherwise mix a learning sequence, task
instructions, lookup material, and design explanation. Do not add placeholder
pages solely to make the directory tree look complete.

## Keep one source of truth

Edit Markdown under `docs/` and the navigation in `mkdocs.yml`. The rendered
GitHub Pages site is generated output, not another documentation source. Do not
maintain parallel copies in a GitHub Wiki, generated site directory, README, or
other publication target.

Preserve existing page URLs when practical. Prefer a navigation-only move when
content needs a different public or maintainer grouping and its physical path
does not cause a concrete problem.

Keep the main discovery path connected: purpose and scope establishes the
boundary, supported capabilities summarizes the product surface, generator
configuration defines accepted settings, generated output examples provide
complete artifacts, troubleshooting handles failures, and focused how-to
guides show frontend and output workflows.

## Use approval-backed generated examples

Document generated output from real integration fixtures. Link the fixture,
focused ApprovalTest, and complete approved artifact from the
[generated output example catalog](../reference/generated-output-examples.md).
Do not duplicate complete generated files in Markdown.

Use public configuration names from the
[generator configuration reference](../reference/generator-configuration.md).
An example should make the relationship between input, configuration, test,
and approved output inspectable. Update an approval only when production output
changes deliberately; documentation-only wording is not a reason to rewrite an
approval.

## Check meaningful drift

Automate claims only when the check has a precise production source:

- verify finite configuration values against the typed configuration model
  when those values change;
- keep the validation rule inventory aligned with stable finding codes;
- use integration fixtures, approvals, and compilation tests for generated
  artifacts; and
- check changed local links, anchors, and navigation targets.

Prefer a focused existing test or check over a documentation-specific
framework. A check should protect observable behavior, not merely prove that a
word appears in Markdown.

## Build the site strictly

Run the site build from the repository root:

```sh
mkdocs build --strict
```

Strict mode turns navigation, link, and other documentation warnings into
failures. The pull-request workflow runs the same strict build. Fix the
underlying warning rather than weakening the build or adding a second
documentation pipeline.

## Review explanation prose manually

Architecture and explanation pages require human review. Confirm that they
describe observable behavior and stable boundaries, use the exact supported
AsyncAPI profile, and avoid volatile class call chains or factory
relationships. Verify material behavior claims against current production code
and focused tests.

Automated checks cannot establish whether a design explanation is accurate,
appropriately scoped, or useful to its intended reader.

## Avoid generic synchronization frameworks

Do not introduce:

- a generic Markdown synchronization or documentation-generation framework;
- tests that assert prose contains every class or implementation name;
- a blanket rule requiring documentation changes for every production-code
  change; or
- parallel documentation copies that need synchronization.

When drift risk is real, add the smallest targeted check at the boundary that
owns the fact. Leave architecture prose and product-scope judgments to explicit
human review.
