# Maven

Replace `VERSION` with the published version shown on the
[Maven plugin artifact page](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-maven-plugin).
The plugin requires Java 21. It is built against and tested with Maven 3.9.

## Generate Kotlin models

Add the plugin to your `pom.xml` with one execution for the AsyncAPI document:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>VERSION</version>
    <executions>
        <execution>
            <id>generate-kotlin-models</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
                <modelPackage>com.example.model</modelPackage>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run the execution with:

```sh
mvn generate-sources
```

Generated sources are written to `target/generated-sources/asyncapi` by
default. The plugin registers the directories it writes to as compile source
roots, and generated schema files as resources, so `mvn compile` and
`mvn package` pick them up without further configuration. The goal binds to
`generate-sources` by default.

## Configure models

Use `modelConfig` for model settings. For example, this execution generates Java records:

```xml
<configuration>
    <generatorName>java</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <modelPackage>com.example.model</modelPackage>
    <modelConfig>
        <modelType>java-record</modelType>
    </modelConfig>
</configuration>
```

See [Generate Kotlin and Java models](generate-kotlin-models.md) for the
generated code and its runtime dependencies.

## Generate Spring Kafka clients

Set both package fields on the execution and put client settings in `clientConfig`:

```xml
<configuration>
    <generatorName>kotlin</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <modelPackage>com.example.model</modelPackage>
    <clientPackage>com.example.client</clientPackage>
    <clientConfig>
        <clientType>spring-kafka</clientType>
        <clientContract>interface</clientContract>
        <producer>
            <enabled>true</enabled>
        </producer>
        <consumer>
            <enabled>true</enabled>
        </consumer>
    </clientConfig>
</configuration>
```

`clientConfig` is required when `clientPackage` is configured, and
`modelPackage` is required as well. See
[Generate Spring Kafka clients](generate-spring-kafka-clients.md) for every
client setting.

## Share settings across executions

Settings in the plugin-level `<configuration>` apply to every execution, which
is useful for one `clientConfig` shared by several contracts. An execution
without `clientPackage` ignores inherited client settings and prints a
warning. Silence it for executions that intentionally generate no client:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>VERSION</version>
    <configuration>
        <clientConfig>
            <clientType>spring-kafka</clientType>
            <clientContract>interface</clientContract>
        </clientConfig>
    </configuration>
    <executions>
        <execution>
            <id>accounts-client</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/accounts.yaml</inputSpec>
                <modelPackage>com.example.accounts.model</modelPackage>
                <clientPackage>com.example.accounts.client</clientPackage>
            </configuration>
        </execution>
        <execution>
            <id>accounts-avro</id>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>avro-schema</generatorName>
                <inputSpec>${project.basedir}/src/main/resources/accounts.yaml</inputSpec>
                <schemaPackage>com.example.accounts.avro</schemaPackage>
                <suppressedWarnings>
                    <warning>unused-client-config</warning>
                </suppressedWarnings>
            </configuration>
        </execution>
    </executions>
</plugin>
```

`suppressedWarnings` can also be set at plugin level. Unknown warning ids fail
the build. See [Configuration warnings](../reference/generator-configuration.md#configuration-warnings).

## Generate schemas

Select a schema generator profile and set `schemaPackage`. This example generates Avro schema files without runtime models:

```xml
<configuration>
    <generatorName>avro-schema</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <outputDirectory>${project.build.directory}/generated-schemas/avro</outputDirectory>
    <schemaPackage>com.example.schema.avro</schemaPackage>
</configuration>
```

The schema-only generator profiles are `avro-schema`, `protobuf-schema`, and
`json-schema`. See [Generate Avro schemas](generate-avro-schemas.md),
[Generate Protobuf schemas and models](generate-protobuf-schemas.md), and
[Generate JSON schemas](generate-json-schemas.md).

## Use a local protoc

Executions with `modelType` `protobuf-message` resolve
`com.google.protobuf:protoc` through the project's repositories, so your
`settings.xml` mirrors, proxies, and credentials apply. Other executions never
resolve it. To use a local executable instead, set `protocExecutable`:

```xml
<configuration>
    <protocExecutable>/opt/protoc/bin/protoc</protocExecutable>
</configuration>
```

or pass `-Dasyncapi.protocExecutable=/opt/protoc/bin/protoc` on the command
line.

## Stale generated files

The plugin records the files each execution generates in
`target/asyncapi-generator/output-manifest.txt`. When a later run of the same
execution no longer generates a file, for example because a schema was removed
from the contract, the file is deleted, unless another execution still
generates it. Files the plugin did not generate are never deleted, and the
bundled `outputFile` is not managed. See
[Stale generated files](../reference/generator-configuration.md#stale-generated-files).

## Execution fields

| Field                | Description                                                                                                             | Default                                                         |
|----------------------|-------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `generatorName`      | Generator profile (`kotlin`, `java`, `avro-schema`, `protobuf-schema`, `json-schema`, `asyncapi-yaml`, `asyncapi-json`) | required                                                        |
| `inputSpec`          | Path to the AsyncAPI YAML or JSON file                                                                                  | required                                                        |
| `outputDirectory`    | Directory for generated sources and schemas                                                                             | `target/generated-sources/asyncapi`                             |
| `outputFile`         | File for bundled document output                                                                                        | none                                                            |
| `modelPackage`       | Package for generated payload models                                                                                    | none                                                            |
| `clientPackage`      | Package for generated client contracts                                                                                  | none                                                            |
| `schemaPackage`      | Package for generated schema artifacts                                                                                  | none                                                            |
| `modelConfig`        | Model implementation and annotation settings                                                                            | none                                                            |
| `clientConfig`       | Spring Kafka client settings                                                                                            | required when `clientPackage` is configured                     |
| `suppressedWarnings` | Configuration warning ids to silence, each in a `<warning>` element                                                     | none                                                            |
| `protocExecutable`   | Local `protoc` for native Protobuf models (property `asyncapi.protocExecutable`)                                        | `com.google.protobuf:protoc:3.25.5` from the project repositories |
