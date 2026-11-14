# Development setup

How to build and test the project locally. You need Java 21+ and Maven 3.9+.

## Fast loop: core

Most work happens in `asyncapi-generator-core`. Its unit tests run in about
twenty seconds:

```bash
mvn test -pl asyncapi-generator-core
```

## Full check: system tests

The `asyncapi-generator-system-test` module runs every scenario through the
packaged CLI, a real Maven build, and real Gradle builds with the Kotlin and
the Groovy DSL. The Maven and Gradle Kotlin DSL builds compile the generated
code against the library versions declared in the root `pom.xml` (Spring Kafka,
Jakarta Validation, Jackson annotations, Kotlin) and run the scenario's checks;
all frontends must produce identical output. A separate test runs repeated
Gradle builds to cover the configuration cache and up-to-date checks. The module
is only built with the `system-test` profile and is never published.

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

CI runs the system tests with the project's own JDK and Gradle versions; bump
them there together with the project.

Consumer projects, generated output, and build logs are kept under
`asyncapi-generator-system-test/target/system-test-work/<scenario>/` for
inspection after a failure.

### Adding a scenario

1. Create `asyncapi-generator-system-test/src/test/scenarios/<name>/spec/` with
   the AsyncAPI documents and every file they reference. Put hand-written
   consumer sources the generated code needs (such as a custom model
   annotation) under `<name>/main/`.
2. Optionally add Kotlin tests under `<name>/checks/`. They are compiled against
   the generated code inside each consumer build, so assert behavior: call
   generated methods, serialize payloads, inspect annotations by reflection.
   Helpers in `src/test/check-support/` read bundled documents and classpath
   resources.
3. Register the scenario in `Scenarios.kt` with one or more executions, the
   client settings, and the extra libraries it needs (Spring Kafka, Avro,
   Protobuf). Set `sharedClientConfig` to configure the client settings once
   for every execution, `expectedWarnings` for warnings every frontend must
   print, or `expectedFailure` for a diagnostic every frontend must report.
   Scenarios can share files through `files`; the configuration diagnostics
   all use one contract.

Besides the checks, every scenario asserts that the frontends generate
identical output, that executions without a package write no code, and that
code generated from a bundled `outputFile` matches code generated from the
original contract. A test that fails because of a known bug is listed in the
scenario's `knownIssues`; it must keep failing until the bug is fixed.
