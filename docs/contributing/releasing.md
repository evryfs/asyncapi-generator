# Publish a release

Use this guide after completing the [release documentation review](release-readiness.md).
The Release workflow publishes Maven and Gradle artifacts, tests public consumer
resolution and CLI distributions, then publishes the prepared GitHub release.

## Confirm prerequisites

The release workflow and its supporting scripts must be merged into `main`.
You need repository write access to prepare the draft, push the release tag,
and dispatch the workflow.

Configure these GitHub Actions repository secrets:

| Secret | Value |
|--------|-------|
| `OSSRH_USERNAME` | Central Publisher Portal user-token username. |
| `OSSRH_TOKEN` | Matching Central Publisher Portal user-token password. |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key for Maven and Gradle artifacts. |
| `GPG_PASSPHRASE` | Passphrase for the signing key. |
| `GRADLE_PUBLISH_KEY` | Plugin Portal API key. |
| `GRADLE_PUBLISH_SECRET` | Matching Plugin Portal API secret. |

The `OSSRH_*` names are retained configuration names; supply current Central
Portal credentials. GitHub supplies `GITHUB_TOKEN` automatically. The workflow
requests repository contents write permission for draft lookup and finalization.

## Prepare the draft and tag

Replace `VERSION` with the intended artifact version and `RELEASE_COMMIT` with
the reviewed commit's full SHA. Tag names must be `vX.Y.Z` or
`vX.Y.Z-prerelease`, such as `v0.3.5-BETA`. Snapshot versions, build metadata
(`+...`), and leading zeros in numeric version identifiers are rejected.

1. Create a draft on the repository's [GitHub releases page](https://github.com/evryfs/asyncapi-generator/releases).
   Set its intended tag to `vVERSION`, add the reviewed release notes, and save
   the draft. Leave it unpublished.
2. Create and push the actual Git tag at the reviewed commit on `main`:

```sh
git fetch origin main &&
git merge-base --is-ancestor RELEASE_COMMIT origin/main &&
git tag -a vVERSION RELEASE_COMMIT -m "Release vVERSION" &&
git push origin refs/tags/vVERSION
```

The sequence stops if any command fails. If the tag already exists, confirm
that it selects the reviewed commit instead of recreating it. Saving a draft's
tag selection is not proof that the Git ref exists; the workflow requires the
explicitly pushed tag and never creates one. Both annotated and lightweight
tags are accepted. Keep the tag and draft identity unchanged during the run.

The workflow applies the release version in each build checkout. It does not
commit version changes back to the repository. A version suffix sets GitHub's
prerelease flag automatically; the draft's initial toggle does not override it.
Keep the project's documented beta status unless a separate decision changes it.

## Dispatch from main

In GitHub Actions, select **Release**, choose **Run workflow**, select the
`main` branch, and enter the existing `vVERSION` tag. Alternatively, from the
repository checkout with GitHub CLI authenticated:

```sh
gh workflow run release.yml --ref main -f tag=vVERSION
```

Dispatch starts real publication after verification. The workflow validates the
tag against `main`'s history at dispatch, finds the matching draft with nonempty
notes, and records the release commit SHA and draft ID. Later jobs use that SHA.

## Follow the release gates

| Job | Required result |
|-----|-----------------|
| `prepare` | Valid version, existing tag on `main`, and matching draft with notes. |
| `verify` | Complete Java 21 `clean install` at the release version, including Maven, Gradle, and packaged-JAR tests; CLI archives and checksums retained. |
| `maven-central` | Parent, core, CLI, and Maven plugin published; Central deployment reaches `PUBLISHED`. |
| `gradle-central` | Gradle implementation and plugin marker uploaded through Sonatype, then closed and released. |
| `gradle-portal` | Gradle plugin submitted to Plugin Portal independently of the Sonatype destination. |
| `published-artifacts` | Fresh Maven and Gradle caches resolve the public release and generate a representative model. Central and Portal are checked independently; Portal fallback to Central is rejected. The Central CLI JAR also runs. |
| `cli-distributions` | Retained archive checksums match; unpacked Linux and Windows launchers run `--version` and generate a model. |
| `finalize` | Tag and draft rechecked, tested assets attached and downloaded bytes verified, then draft published with version-derived prerelease status. |

The two Gradle publication jobs run after Maven Central, so core is already
published. Sonatype staging completion alone does not establish public
availability; the consumer tests provide that gate. Repository probes wait up
to ten minutes per artifact for public availability. Portal approval delays
can therefore require a later smoke-test retry.

The published GitHub release contains these CLI assets:

```text
asyncapi-generator-VERSION-bin.tar.gz
asyncapi-generator-VERSION-bin.zip
SHA256SUMS
```

These archives are GitHub assets only. Maven Central continues to provide the
executable CLI JAR, sources, and Javadoc. See [CLI installation](../how-to/cli.md).

## Recover a failed run

Read the failed job's logs and step summary before retrying. Once publication
has started, keep successful publication jobs completed; use GitHub's failed-job
or individual-job rerun controls instead of rerunning all jobs or dispatching a
new release attempt.

| Failure | Recovery |
|---------|----------|
| `prepare` or `verify` | Before any publication, resolve the draft or build failure and rerun the failed job, or dispatch again with corrected inputs. Source changes need a newly reviewed release commit and tag decision. |
| Maven Central publication | Inspect the logged deployment ID in Central Portal. A timeout can occur after upload or publication. Resolve that deployment before attempting another upload. |
| Gradle Central publication | Inspect the staging repository ID and its Central deployment. `publishToSonatype` creates a new staging repository; rerunning it does not resume an earlier upload. |
| Plugin Portal publication | Inspect the version's upload and approval state. Activated versions cannot be overwritten. |
| Public consumer or distribution smoke test | Resolve availability, approval, or consumer failures, then rerun the failed smoke job. Successful publication jobs need no repeat upload. |
| Finalization | Keep the same tag, draft, and retained artifacts. Matching uploaded assets are reused; incomplete or conflicting assets fail verification and require inspection before retrying. |

For a confirmed complete Gradle staging upload that only needs closing or
releasing, the pinned Nexus plugin supports resuming by repository ID. From
`asyncapi-generator-gradle-plugin`, with the same Central credentials available:

```sh
./gradlew -PasyncApiGeneratorVersion=VERSION closeSonatypeStagingRepository --staging-repository-id STAGING_ID
./gradlew -PasyncApiGeneratorVersion=VERSION releaseSonatypeStagingRepository --staging-repository-id STAGING_ID
```

Use these only after inspecting that exact staging repository. They do not
upload missing artifacts or turn a failed Actions job into a successful one.

The workflow has no input for adopting an already published deployment. If a
destination published successfully but its job failed, stop for maintainer
recovery: an ordinary upload rerun cannot acknowledge that success. Likewise,
if finalization timed out after GitHub published the draft, inspect the public
release and assets before any further action; the finalizer requires a draft.
Do not hide partial publication with `ignorePublishedComponents` or move an
existing release tag to different source.

Upstream references: [GitHub job reruns](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/re-run-workflows-and-jobs),
[Nexus staging recovery](https://github.com/gradle-nexus/publish-plugin/blob/v2.0.0/README.md#publishing-and-closing-in-different-gradle-invocations),
and [Plugin Portal publication and approval](https://plugins.gradle.org/docs/publish-plugin).
