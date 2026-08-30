# Review release documentation

Use this guide to review release-facing documentation and GitHub release notes
before publishing a release. This is an evergreen maintainer checklist: begin
each release review with unchecked items, and do not commit release-specific
checked state to this page.

Completing the checklist is a release gate, not a claim that the project or a
particular release is ready for stable use. Keep the project's beta status
truthful unless a separate decision changes it.

## Confirm release scope and status

- [ ] Define the user-visible behavior and supported capabilities included in
      the proposed release.
- [ ] Compare the README and
      [supported capabilities](../reference/supported-capabilities.md) with
      current production code, focused tests, and approvals.
- [ ] Confirm that retained limitations and intentionally deferred work remain
      explicit and accurate.

## Review project entry points

- [ ] Check the README's beta status, badges, project identity, canonical
      documentation links, and Maven, Gradle, and CLI quick starts.
- [ ] Confirm that Maven, Gradle, and CLI examples describe equal frontends over
      the same core behavior, including their actual tasks, commands, and
      default output directories.
- [ ] Confirm that generator and plugin usage examples use `VERSION` and that
      each containing page tells readers where to find the published version.
- [ ] Preserve literal contract versions such as `info.version` when they are
      message-contract metadata rather than generator versions.

## Review generator configuration

- [ ] Compare public option names across Maven XML, the Gradle DSL, and CLI
      flags with the typed frontend configuration and focused frontend tests.
- [ ] Verify generator profile names, model-type names, defaults, and accepted
      finite values against the typed core configuration.
- [ ] Verify required package, model, client, schema, and output-file
      combinations, including how each frontend maps them to core behavior.
- [ ] Confirm schema documentation distinguishes Avro Projection, native Avro,
      native Protobuf, and JSON Schema output modes and describes their runtime
      model behavior accurately.

## Review Spring Kafka guidance

- [ ] Verify the required model package, client package, `spring-kafka` client
      type, and `interface` contract configuration for every frontend.
- [ ] Check producer and consumer defaults, independent enablement, and the
      additive `byte-array` and `string` producer payload types against focused
      tests and approvals.
- [ ] Confirm the documentation describes generated clients as interfaces that
      applications implement and explains that applications provide Spring and
      Kafka runtime configuration.

## Review generated examples

- [ ] Confirm every generated example links to a real input fixture, focused
      ApprovalTest, and current approved artifact.
- [ ] Use approved files as the complete generated examples; do not duplicate
      complete generated artifacts in documentation pages.

## Validate documentation

- [ ] Check changed local links, anchors, and `mkdocs.yml` navigation entries.
- [ ] Run `mkdocs build --strict` from the repository root and resolve every
      warning or error.
- [ ] Run `git diff --check` and review the complete release-documentation diff
      for unrelated or unsupported claims.

## Prepare GitHub release notes

GitHub release notes are the repository's release-note surface. Publishing a
GitHub release triggers `.github/workflows/release.yml`, so documentation and
release-note review must finish before the release is published.

- [ ] Describe user-visible behavior and supported-capability changes.
- [ ] Identify breaking API or configuration changes.
- [ ] State retained limitations and intentionally deferred work.
- [ ] Confirm that artifact and version identity match the release tag.
- [ ] Link relevant canonical documentation under `docs/`.
- [ ] Review and approve the complete GitHub release notes before publishing
      the release.

## Run the final repository gate

- [ ] Confirm that the active runtime is Java 21:

```sh
java -version
```

- [ ] Run the complete repository verification from the repository root with
      Java 21 active:

```sh
mvn -q clean install
```

- [ ] Resolve every documentation or repository verification failure before
      approving release publication.
