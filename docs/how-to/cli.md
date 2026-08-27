# CLI

Download the CLI from [Maven Central](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-cli) or build it from source.

## Generate Kotlin models

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model
```

Generated sources are written to `./generated/asyncapi` by default.

## Generation and output options

| Option                     | Description                                        |
|----------------------------|----------------------------------------------------|
| `--input-spec`, `-i`       | AsyncAPI YAML or JSON file (required)              |
| `--generator-name`, `-g`   | Generator profile (required)                       |
| `--output-directory`, `-o` | Output directory (default: `./generated/asyncapi`) |
| `--output-file`            | File for bundled document output                   |
| `--model-package`          | Package for generated payload models               |
| `--client-package`         | Package for generated client contracts             |
| `--schema-package`         | Package for generated schema artifacts             |

## Model options

| Option               | Description                                                                                                            |
|----------------------|------------------------------------------------------------------------------------------------------------------------|
| `--model-annotation` | Fully qualified annotation for generated models                                                                        |
| `--model-type`       | Model implementation (`kotlin-data-class`, `java-class`, `java-record`, `avro-specific-record`, or `protobuf-message`) |

## Client options

| Option                                      | Description                                                         |
|---------------------------------------------|---------------------------------------------------------------------|
| `--client-type`                             | Client technology (`spring-kafka`)                                  |
| `--client-contract`                         | Contract shape (`interface`)                                        |
| `--generate-producer`                       | Enable producer generation                                          |
| `--no-generate-producer`                    | Disable producer generation                                         |
| `--generate-consumer`                       | Enable consumer generation                                          |
| `--no-generate-consumer`                    | Disable consumer generation                                         |
| `--producer-additional-payload-type`        | Additional payload representation (`byte-array` or `string`)        |
| `--topic-parameter-property`                | Map a channel parameter to a Spring property (`PARAMETER=PROPERTY`) |
| `--client-contract-validation-annotation`   | Fully qualified validation annotation for client contracts          |
| `--payload-parameter-validation-annotation` | Fully qualified validation annotation for payload parameters        |

## Generate Spring Kafka clients

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model \
  --client-package com.example.client \
  --client-type spring-kafka \
  --client-contract interface
```

## Generate schemas

Select a schema generator profile and provide the schema package. This example generates Avro schema files without runtime models:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name avro-schema \
  --schema-package com.example.schema.avro
```

The schema-only generator profiles are `avro-schema`, `protobuf-schema`, and `json-schema`.

## Bundle a document

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name asyncapi-yaml \
  --output-file target/bundled-asyncapi.yaml
```
