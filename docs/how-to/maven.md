# Maven

Replace `VERSION` with the published version shown on the
[Maven plugin artifact page](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-maven-plugin).

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

Generated sources are written to `target/generated-sources/asyncapi` by default.

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

`clientConfig` is required when `clientPackage` is configured.

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

The schema-only generator profiles are `avro-schema`, `protobuf-schema`, and `json-schema`.

## Execution fields

| Field             | Description                                                                                                             | Default                                     |
|-------------------|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| `generatorName`   | Generator profile (`kotlin`, `java`, `avro-schema`, `protobuf-schema`, `json-schema`, `asyncapi-yaml`, `asyncapi-json`) | required                                    |
| `inputSpec`       | Path to the AsyncAPI YAML or JSON file                                                                                  | required                                    |
| `outputDirectory` | Directory for generated sources and schemas                                                                             | `target/generated-sources/asyncapi`         |
| `outputFile`      | File for bundled document output                                                                                        | none                                        |
| `modelPackage`    | Package for generated payload models                                                                                    | none                                        |
| `clientPackage`   | Package for generated client contracts                                                                                  | none                                        |
| `schemaPackage`   | Package for generated schema artifacts                                                                                  | none                                        |
| `modelConfig`     | Model implementation and annotation settings                                                                            | none                                        |
| `clientConfig`    | Spring Kafka client settings                                                                                            | required when `clientPackage` is configured |
