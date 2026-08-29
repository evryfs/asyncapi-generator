# asyncapi-generator

![Build](https://img.shields.io/github/actions/workflow/status/evryfs/asyncapi-generator/build.yml?branch=main&label=build)
![Release](https://img.shields.io/github/actions/workflow/status/evryfs/asyncapi-generator/release.yml?branch=main&label=release)
![Maven Central](https://img.shields.io/maven-central/v/dev.banking.asyncapi.generator/asyncapi-generator-maven-plugin)
![AsyncAPI 3.0.x](https://img.shields.io/badge/AsyncAPI-3.0.x-purple)
![Coverage](https://img.shields.io/badge/coverage-report-blue)
![License](https://img.shields.io/github/license/evryfs/asyncapi-generator)

`asyncapi-generator` is an independent generator for the
[supported AsyncAPI 3.0.x profile](https://evryfs.github.io/asyncapi-generator/reference/supported-capabilities/).
It produces self-contained bundled AsyncAPI documents, Kotlin and Java models,
schema artifacts, and Spring Kafka producer and consumer interface contracts
from supported YAML and JSON contracts. It does not configure or run Kafka
infrastructure.

This repository is not the
[official AsyncAPI Generator](https://github.com/asyncapi/generator).

## Quick start

With Java 21+, Maven 3.9+, and a small AsyncAPI document at
`src/main/resources/asyncapi.yaml`, add this plugin execution to `pom.xml`:

```xml
<plugin>
    <groupId>dev.banking.asyncapi.generator</groupId>
    <artifactId>asyncapi-generator-maven-plugin</artifactId>
    <version>{latest-version}</version>
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

Run generation:

```shell
mvn generate-sources
```

Generated Kotlin files are written below
`target/generated-sources/asyncapi/com/example/model` by default. Follow the
[getting-started tutorial](https://evryfs.github.io/asyncapi-generator/tutorials/getting-started/)
for a complete contract, generated example, and explanation.

## Frontends

Maven, Gradle, and the CLI expose the same core generation behavior with
frontend-specific configuration and output defaults.

| Frontend | Guide                                                                       | Default generated-file directory            |
|----------|-----------------------------------------------------------------------------|---------------------------------------------|
| Maven    | [Maven how-to](https://evryfs.github.io/asyncapi-generator/how-to/maven/)   | `target/generated-sources/asyncapi`         |
| Gradle   | [Gradle how-to](https://evryfs.github.io/asyncapi-generator/how-to/gradle/) | `build/generated/asyncapi/<execution-name>` |
| CLI      | [CLI how-to](https://evryfs.github.io/asyncapi-generator/how-to/cli/)       | `./generated/asyncapi`                      |

See the
[configuration reference](https://evryfs.github.io/asyncapi-generator/reference/generator-configuration/)
for generator profiles, accepted values, required combinations, and output
configuration.

## Documentation

| Need                                      | Canonical documentation                                                                                       |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Complete a first generation run           | [Getting started](https://evryfs.github.io/asyncapi-generator/tutorials/getting-started/)                     |
| Check supported workflows and limitations | [Supported capabilities](https://evryfs.github.io/asyncapi-generator/reference/supported-capabilities/)       |
| Inspect generated artifacts               | [Generated output examples](https://evryfs.github.io/asyncapi-generator/reference/generated-output-examples/) |
| Configure generation                      | [Generator configuration](https://evryfs.github.io/asyncapi-generator/reference/generator-configuration/)     |
| Diagnose failures                         | [Troubleshooting](https://evryfs.github.io/asyncapi-generator/how-to/troubleshooting/)                        |
| Build and contribute                      | [Development setup](https://evryfs.github.io/asyncapi-generator/contributing/development-setup/)              |
| Review licensing terms                    | [Apache License 2.0](LICENSE)                                                                                 |
