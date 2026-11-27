# Development setup

How to build and test the project locally. You need Java 21+ and Maven 3.9+.

## Fast loop: core

Most work happens in `asyncapi-generator-core`. Its unit tests run in about
twenty seconds:

```bash
mvn test -pl asyncapi-generator-core
```

## Formatting and static analysis

`mvn verify` runs ktlint and detekt. Format the sources before committing:

```bash
mvn ktlint:format -pl asyncapi-generator-core
```

The rules and the detekt policy are described in the
[code conventions](code-conventions.md).

## Full check: system tests

The `asyncapi-generator-system-test` module runs every scenario through the
packaged CLI, a real Maven build, and real Gradle builds with the Kotlin and
the Groovy DSL. The Maven and Gradle Kotlin DSL builds compile the generated
code against the library versions declared in the root `pom.xml` (Spring Kafka,
Jakarta Validation, Jackson annotations, Avro, Protobuf, Kotlin) and run the
scenario's checks; all frontends must produce identical output. Separate tests
run repeated Gradle builds to cover the configuration cache and up-to-date
checks, and repeated Maven and Gradle builds to cover the removal of stale
generated files. The module is only built with the `system-test` profile and
is never published.

```bash
mvn verify -Psystem-test -pl asyncapi-generator-system-test -am
```

Add `-Dinvoker.skip=true` to skip the Maven plugin's own integration tests and
`-Dsystem-test.only=<scenario>,<scenario>` to run only some scenarios while
iterating. Override versions for a compatibility check with, for example:

```bash
-Dspring-kafka.version=<version>
-Dsystem-test.gradle.version=<version>
```

CI runs the system tests with the project's own JDK and Gradle versions
(`system-test.gradle.version` in `asyncapi-generator-system-test/pom.xml`);
bump them there together with the project. The `system-test` job of the Build
workflow runs them for pushes to `main` and for pull requests, and the Release workflow
runs them in its `verify` job before anything is published.

Consumer projects, generated output, and build logs are kept under
`asyncapi-generator-system-test/target/system-test-work/<scenario>/` for
inspection after a failure.

### Adding a scenario

Each scenario is a folder under
`asyncapi-generator-system-test/src/test/scenarios/`; the
[scenario overview](https://github.com/evryfs/asyncapi-generator/tree/main/asyncapi-generator-system-test/src/test/scenarios)
lists them and describes the layout.

1. Create `<name>/spec/` with the AsyncAPI documents and every file they
   reference. Put hand-written consumer sources the generated code needs (such
   as a custom model annotation) under `<name>/main/`.
2. Write the builds: `<name>/maven/pom.xml`, `<name>/gradle/build.gradle.kts`,
   and `<name>/gradle/build.gradle`. Copy them from a similar scenario. They
   take versions from `${<name>.version}` properties (Maven) and the `libs`
   version catalog (Gradle), which the system tests provide, and they declare
   only the libraries the scenario needs (Spring Kafka, Avro, Protobuf).
3. Optionally add Kotlin tests under `<name>/checks/`. They are compiled against
   the generated code inside each consumer build, so assert behavior: call
   generated methods, serialize payloads, inspect annotations by reflection.
   Helpers in `src/test/check-support/` read bundled documents and classpath
   resources.
4. Register the scenario in `Scenarios.kt` with the same executions and client
   settings as the build files; the CLI runs from these, and all frontends must
   generate identical output. Set `sharedClientConfig` when the build files
   configure the client settings once for every execution, `expectedWarnings`
   for warnings every frontend must print, `absentWarnings` for warnings no
   frontend may print, or `expectedFailure` for a diagnostic every frontend
   must report.

Besides the checks, every scenario asserts that the frontends generate
identical output, that executions without a package write no code, and that
code generated from a bundled `outputFile` matches code generated from the
original contract. A test that fails because of a known bug is listed in the
scenario's `knownIssues`; it must keep failing until the bug is fixed.
