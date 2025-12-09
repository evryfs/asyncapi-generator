# asyncapi-generator

The asyncapi-generator is an open-source tool for generating code and schemas from AsyncAPI Yaml specifications.

The project is currently in BETA.

## Supported generators

- Kotlin - Data classes with Jakarta Validation annotations
- Java - POJOs with Jakarta Validation annotations
- Spring Kafka - Producer and Consumer templates for both Kotlin and Java
- Avro - Schema generation from AsyncAPI schemas

The current documentation provided is still a draft, found in `docs/` folder at the repository root.

## Usage

Currently, the asyncapi-generator BETA version is available as a maven plugin through maven central.

Example usage in your `pom.xml`:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>0.0.1</version> <!-- current BETA version -->
    <executions>
        <execution>
            <id>generate-example</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
            <configuration>
                <generatorName>kotlin</generatorName> <!-- options: kotlin, java - default kotlin -->
                <inputFile>path/to/my/asyncapi_specification.yaml</inputFile>
                <modelPackage>my.package.path.model</modelPackage> <!-- package name for data class/POJO -->
                <clientPackage>my.package.path.client</clientPackage> <!-- package name for kafka client - default modelPackage-->
                <schemaPackage>my.package.path.schema</schemaPackage> <!-- package name for avro schema - default modePackage -->
                <configuration>
                    <generateModels>true</generateModels> <!-- can skip models - default is true -->
                    <generateSpringKafkaClient>true</generateSpringKafkaClient> <!-- default is false -->
                    <generateAvroSchema>true</generateAvroSchema> <!-- default is false -->
                </configuration>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Features supported

### Parser

The core parsing logic is stable and handles the structural validation of AsyncAPI documents.

- [x] **AsyncAPI YAML Support:** Reads and Parses yaml format.
- [ ] **AsyncAPI JSON Support:** Reads and Parses yaml format.
- [x] **Context-Aware Error Handling:** Provides precise error messages with line numbers and JSON paths.
- [x] **Reference Resolution:** Supports internal and external file references.
- [x] **Components:** Full parsing support for Schemas, Messages, Channels, Parameters, etc.

### Schema Formats

- [x] **Yaml Schema:** Fully supported (default format).
- [ ] **Multi-Format Schemas:**
  - [ ] **JSON Schema:** Support for parsing JSON schemas defined in AsyncAPI documents.
  - [ ] **Avro Schema:** Support for parsing Avro schemas defined in AsyncAPI documents.
  - [ ] **Protobuf Schema:** Support for parsing Protobuf schemas defined in AsyncAPI documents.
  - [ ] **RAML Schema:** Support for parsing Protobuf schemas defined in AsyncAPI documents.

### Validator

- [x] **Structural Validation:** Ensures the AsyncAPI document adheres to the AsyncAPI specification.
- [x] **Context-Aware Error Handling:** Provides precise error messages with line numbers and context.
- [x] **Warnings:** Provides warnings for best practices and potential issues.
- [ ] **Formatted Warnings:** Enhanced warning messages with suggestions for improvement.

## Future plans

- [ ] **Stable Release:** Move from BETA to stable release with comprehensive testing.
- [ ] **CLI Tool:** Publish the already made CLI module to package managers like brew and dnf.
- [ ] **Documentation:** Complete documentation with examples and guides.
- [ ] **Additional Generators:** Expand support for more programming languages and frameworks, i.e., Quarkus Kafka.
- [ ] **Enhanced Schema Support:** Full support for multi-format schemas including Avro and Protobuf.
- [ ] **Serialization:** Consider using kotlinx-serialization for writing bundled schemas to files.
