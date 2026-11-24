# System-test scenarios

Every folder here is a small consumer project that the system tests build with
the Maven plugin, the Gradle plugin (Kotlin and Groovy DSL), and the CLI. The
build files are the configuration under test, so they double as working
examples of how to configure the plugins.

| Part | Contents |
|------|----------|
| `spec/` | The AsyncAPI contract and every file it references. In the consumer project it is copied to `src/main/asyncapi/`. |
| `maven/pom.xml` | The Maven build. |
| `gradle/build.gradle.kts` | The Gradle build in the Kotlin DSL. |
| `gradle/build.gradle` | The same Gradle build in the Groovy DSL. |
| `main/` | Hand-written sources the generated code needs, such as a custom annotation. |
| `checks/` | Kotlin tests compiled against the generated code and run inside the consumer build. |

Versions are not written into the build files. The pom reads them from
properties such as `${asyncapi-generator.version}` and `${kotlin.version}`,
which the system tests pass on the command line. The Gradle builds read them
from the `libs` version catalog, which the system tests write next to the
build file. Repositories and the Gradle settings file are also added by the
system tests. In your own project you would declare these the usual way.

The CLI configuration of each scenario lives in
[`Scenarios.kt`](../kotlin/dev/banking/asyncapi/generator/systemtest/Scenarios.kt),
together with the warnings and failures every frontend must report. All
frontends must generate identical output, so the build files and `Scenarios.kt`
have to describe the same configuration.

## Scenarios

| Scenario | What it shows |
|----------|---------------|
| [spring-kafka-contract](spring-kafka-contract) | Kotlin models and Spring Kafka producer and consumer interfaces, with extra producer payload types, topic parameter properties, and validation annotations. |
| [java-client](java-client) | Java models with a Spring Kafka client, and Java Protobuf messages from a native schema. |
| [producer-only](producer-only) | A client with only producer contracts. |
| [complete-configuration](complete-configuration) | Every generator in one build: Kotlin client, Java records, Avro and Protobuf models from native schemas, Avro, Protobuf, and JSON schemas, YAML and JSON documents, custom output directories, and a model annotation. |
| [output-locations](output-locations) | Custom output directories for sources and resources, and several contracts in one build. |
| [shared-client-config](shared-client-config) | Client configuration shared by all executions; executions without a client package print the `unused-client-config` warning. |
| [suppressed-warnings](suppressed-warnings) | The same build with the warning silenced through `suppressedWarnings`. |
| [generated-code](generated-code) | Kotlin and Java models for enums, `oneOf` with a discriminator, defaults, and property names. |
| [avro-schemas](avro-schemas) | Avro schemas for recursive types, defaults, and escaped documentation. |
| [avro-schema-types](avro-schema-types) | Avro schemas use `long` for wide integers and inline `anyOf` unions. |
| [generated-code-edge-cases](generated-code-edge-cases) | Kotlin and Java models for date defaults, fieldless objects, escaped discriminators, snake_case union members, and unions without a discriminator; prints the indistinguishable-members warning. |
| [kafka-payload-types](kafka-payload-types) | Spring Kafka clients for array and 64-bit integer payloads, in Kotlin and Java. |
| [kafka-scalar-payloads](kafka-scalar-payloads) | Spring Kafka clients for UUID, date, date-time and number payloads, typed as models type them, in Kotlin and Java. |
| [union-discriminator-values](union-discriminator-values) | Kotlin and Java unions whose discriminator values come from `const` and from `allOf`-narrowed enums. |
| [inline-union-properties](inline-union-properties) | Kotlin and Java union types for inline `oneOf` and `anyOf` properties. |
| [bundling](bundling) | Bundling a contract split over several files into one `outputFile`, and the `asyncapi-yaml` generator. |
| [bundle-round-trip](bundle-round-trip) | A multi-file contract with reply channels, reference chains, tuple items, deep pointers, same-named files, and `x-*` extensions, bundled and regenerated from the bundle. |
| [external-references](external-references) | `$ref` forms across files, external messages, and recursive schemas, for Kotlin and Java. |
| [stale-generated-files](stale-generated-files) | Regenerating after a schema is removed deletes its generated files (`StaleGeneratedFilesSystemTest`). |
| [dotted-component-names](dotted-component-names) | Component names with dots, such as `eu.prod`, are accepted in every components map. |
| [gradle-incremental-build](gradle-incremental-build) | Configuration cache and up-to-date checks (`GradleIncrementalBuildSystemTest`). |

The `diagnostics-*` scenarios each contain one mistake, in the configuration or
in the contract, that every frontend must reject with a clear message:

| Scenario | Mistake |
|----------|---------|
| [diagnostics-missing-output](diagnostics-missing-output) | No package or `outputFile` configured. |
| [diagnostics-missing-input-spec](diagnostics-missing-input-spec) | `inputSpec` points to a file that does not exist. |
| [diagnostics-unknown-generator](diagnostics-unknown-generator) | Unknown `generatorName`. |
| [diagnostics-unknown-model-type](diagnostics-unknown-model-type) | Unknown `modelType`. |
| [diagnostics-document-without-output-file](diagnostics-document-without-output-file) | A document generator without `outputFile`. |
| [diagnostics-unqualified-validation-annotation](diagnostics-unqualified-validation-annotation) | A validation annotation without its package. |
| [diagnostics-client-package-without-client-config](diagnostics-client-package-without-client-config) | `clientPackage` without `clientConfig`. |
| [diagnostics-reference-cycle](diagnostics-reference-cycle) | `$ref`s that form a cycle. |
| [diagnostics-recursive-yaml-alias](diagnostics-recursive-yaml-alias) | A YAML alias that refers to itself. |
| [diagnostics-inline-name-collision](diagnostics-inline-name-collision) | Two different inline schemas that would get the same class name. |
| [diagnostics-property-name-collision](diagnostics-property-name-collision) | Two properties that become the same source identifier. |
| [diagnostics-header-name-collision](diagnostics-header-name-collision) | Kafka headers whose parameter names collide with the fixed consumer parameters. |
| [diagnostics-ref-operation-channel-scope](diagnostics-ref-operation-channel-scope) | A root operation or reply reached through `$ref` that uses a component channel instead of a root channel. |
| [diagnostics-invalid-component-name](diagnostics-invalid-component-name) | A components key with characters the spec does not allow. |
| [diagnostics-invalid-avro-name](diagnostics-invalid-avro-name) | A property name that is not a valid Avro name. |
