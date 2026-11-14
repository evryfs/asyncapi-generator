# Generate Protobuf schemas and models

Protobuf output is generated from native Protobuf schemas in the contract. The
generator does not derive Protobuf from AsyncAPI Schema Objects. Two outputs
are available:

- `.proto` files only, with the `protobuf-schema` generator; and
- `.proto` files plus Java message classes, and for Kotlin the official Kotlin
  DSL, with model type `protobuf-message`.

## Put the Protobuf schema in the contract

Embed the schema, or reference a `.proto` file next to the contract:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.google.protobuf;version=3
      schema: |
        syntax = "proto3";

        package com.example.events;

        option java_package = "com.example.events";
        option java_multiple_files = true;

        message UserCreated {
          string user_id = 1;
          string email = 2;
        }
```

To reference a file instead, write `schema: { $ref: user-created.proto }`.
The accepted formats are `application/vnd.google.protobuf;version=2` and
`application/vnd.google.protobuf;version=3`. Message generation requires a
top-level message with the payload's name, `UserCreated` in this example.

## Generate .proto files

Maven:

```xml
<configuration>
    <generatorName>protobuf-schema</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <schemaPackage>com.example.events.proto</schemaPackage>
</configuration>
```

Gradle:

```kotlin
asyncApiGenerator {
    executions {
        register("proto") {
            generatorName.set("protobuf-schema")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            schemaPackage.set("com.example.events.proto")
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name protobuf-schema \
  --schema-package com.example.events.proto
```

The schema content is written unchanged to
`com/example/events/proto/UserCreated.proto`. `schemaPackage` selects the
directory only; it does not rewrite the `package` declaration.

## Generate Java messages and the Kotlin DSL

Use a `java` or `kotlin` execution with model type `protobuf-message`:

```xml
<configuration>
    <generatorName>kotlin</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <modelPackage>com.example.events</modelPackage>
    <modelConfig>
        <modelType>protobuf-message</modelType>
    </modelConfig>
</configuration>
```

In Gradle set `modelConfig { modelType.set("protobuf-message") }`; in the CLI
pass `--model-type protobuf-message`. The schema must meet two requirements:

- `modelPackage` must equal `option java_package`, or the Protobuf `package`
  when `java_package` is not set; and
- the schema must set `option java_multiple_files = true`.

With `java`, the result is the Java message classes. With `kotlin`, it is the
Java message classes plus the Kotlin DSL; the Java classes remain the runtime
types. The application needs `com.google.protobuf:protobuf-java` 3.25.x, and
for the Kotlin DSL also `com.google.protobuf:protobuf-kotlin` 3.25.x.

## Make protoc available

Message generation runs `protoc` version `3.25.5`, published as the Maven
artifact `com.google.protobuf:protoc`. Only executions with
`protobuf-message` resolve it.

- **Maven** resolves it through the project's repositories, so `settings.xml`
  mirrors, proxies, and credentials apply. Nothing else is needed when your
  repository manager proxies Maven Central.
- **Gradle** resolves it through the `asyncApiProtoc` configuration from the
  project's `repositories`, so the build must declare a repository that
  provides it, such as `mavenCentral()` or your mirror.
- **The CLI** uses the copy in the local Maven repository (`~/.m2/repository`,
  or the `maven.repo.local` system property) and otherwise downloads it from
  Maven Central, checks its SHA-1 checksum, and stores it there. The download
  uses the JVM proxy settings.

On build machines without repository access, install `protoc` yourself and
point the generator at it:

| Frontend | Setting                                                                                       |
|----------|-----------------------------------------------------------------------------------------------|
| Maven    | `<protocExecutable>/opt/protoc/bin/protoc</protocExecutable>` or `-Dasyncapi.protocExecutable=...` |
| Gradle   | `asyncApiGenerator { protocExecutable.set(file("/opt/protoc/bin/protoc")) }`                  |
| CLI      | `--protoc /opt/protoc/bin/protoc`                                                             |

Use a `protoc` 3.25.x release so the generated code matches the Protobuf
runtime.

A complete schema example is linked from
[Native Protobuf schema artifact](../reference/generated-output-examples.md#native-protobuf-schema-artifact).
