# CLI

Use Java 21 and choose a published `VERSION` from the
[GitHub releases page](https://github.com/evryfs/asyncapi-generator/releases).
For releases with portable CLI assets, install the archive as described below.
The executable [Maven Central JAR](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-cli)
is also available, including for earlier releases without portable assets.

## Install on Linux or macOS

Download `asyncapi-generator-VERSION-bin.tar.gz` and `SHA256SUMS` from the same
release. Replace `VERSION` in these commands with the selected artifact version,
without the tag's leading `v`.

Check the archive's SHA-256 hash before unpacking:

```sh
shasum -a 256 asyncapi-generator-VERSION-bin.tar.gz
```

On Linux, `sha256sum` is another option. Compare the result with the matching
archive's entry in `SHA256SUMS`, then unpack and add its `bin` directory to
your current shell's `PATH`:

```sh
tar -xzf asyncapi-generator-VERSION-bin.tar.gz
export PATH="$PWD/asyncapi-generator-VERSION/bin:$PATH"
asyncapi-generator --version
```

## Install on Windows

Download `asyncapi-generator-VERSION-bin.zip` and `SHA256SUMS` from the same
release. In PowerShell, check the archive's hash:

```powershell
Get-FileHash -Algorithm SHA256 .\asyncapi-generator-VERSION-bin.zip
```

Compare it with the matching entry in `SHA256SUMS`. Then unpack and add the
`bin` directory to your current session's `PATH`:

```powershell
Expand-Archive -LiteralPath .\asyncapi-generator-VERSION-bin.zip -DestinationPath .
$env:PATH = "$($PWD.Path)\asyncapi-generator-VERSION\bin;$env:PATH"
asyncapi-generator --version
```

The batch launcher works from PowerShell and Command Prompt when `bin` is on
`PATH`. Both platforms print `asyncapi-generator version VERSION`.

For future terminals, add the absolute `bin` directory to your shell profile
or Windows user `PATH`. Keep the extracted directory intact: the launchers
locate `lib/asyncapi-generator.jar` relative to `bin`. They use
`JAVA_HOME/bin/java` when `JAVA_HOME` is set, otherwise Java on `PATH`.

## Run the Maven Central JAR

Download `asyncapi-generator-cli-VERSION.jar` from the
[CLI artifact page](https://central.sonatype.com/artifact/dev.banking.asyncapi.generator/asyncapi-generator-cli).
The JAR contains its runtime dependencies and accepts the same options as the
portable launchers:

```sh
java -jar asyncapi-generator-cli-VERSION.jar --version
java -jar asyncapi-generator-cli-VERSION.jar \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.model
```

In the remaining examples, replace `asyncapi-generator` with
`java -jar asyncapi-generator-cli-VERSION.jar` when using this alternative.

## Build the distribution from source

From the repository root with Java 21 active:

```sh
mvn -pl asyncapi-generator-cli -am -Pcli-distribution verify
```

The ZIP and tar.gz archives are written to `asyncapi-generator-cli/target`.
Use the version from your checkout in their filenames. The optional
`cli-distribution` profile creates the portable archives; ordinary builds
continue to produce the executable JAR. Archives are GitHub release assets and
are not attached to Maven Central publication.

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
