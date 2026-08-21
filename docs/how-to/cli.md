# CLI

Download the CLI from [Maven Central](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-cli) or build from source.

## Basic usage

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model
```

## Configuration options

| Option                     | Description                                        |
|----------------------------|----------------------------------------------------|
| `--input-spec`, `-i`       | AsyncAPI YAML or JSON file (required)              |
| `--generator-name`, `-g`   | Generator profile (required)                       |
| `--output-directory`, `-o` | Output directory (default: `./generated/asyncapi`) |
| `--output-file`            | File for bundled document output                   |
| `--model-package`          | Package for generated models                       |
| `--client-package`         | Package for generated clients                      |
| `--schema-package`         | Package for generated schemas                      |

## Model options

| Option               | Description                                     |
|----------------------|-------------------------------------------------|
| `--model-annotation` | Fully qualified annotation for generated models |
| `--model-type`       | Model implementation (`class` or `record`)      |

## Client options

| Option                                           | Description                                                     |
|--------------------------------------------------|-----------------------------------------------------------------|
| `--client-type`                                  | Client technology (`spring-kafka`)                              |
| `--client-contract`                              | Contract shape (`interface`)                                    |
| `--generate-producer` / `--no-generate-producer` | Enable/disable producer generation                              |
| `--generate-consumer` / `--no-generate-consumer` | Enable/disable consumer generation                              |
| `--producer-additional-payload-type`             | Additional payload representation (`byte-array`, `string`)      |
| `--topic-parameter-property`                     | Map channel parameter to Spring property (`PARAMETER=PROPERTY`) |

## Schema options

| Option                                            | Description                          |
|---------------------------------------------------|--------------------------------------|
| `--schemas-native-avro`                           | Enable native Avro generation        |
| `--schemas-native-avro-generate-specific-records` | Generate Avro SpecificRecord sources |
| `--schemas-native-protobuf`                       | Enable native Protobuf generation    |

## Examples

Generate Kotlin models and Spring Kafka clients:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model \
  --client-package com.example.client \
  --client-type spring-kafka \
  --client-contract interface
```

Generate Avro schema artifacts:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name avro-schema \
  --schema-package com.example.schema
```

Bundle a multi-file document:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name asyncapi-yaml \
  --output-file target/bundled-asyncapi.yaml
```
