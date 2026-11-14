# Generate JSON schemas

The `json-schema` generator writes one JSON Schema Draft 7 file per named
schema of the contract, for example to validate payloads outside the JVM or
to register them in a schema registry. It generates no model source.

## Configure the execution

Maven:

```xml
<configuration>
    <generatorName>json-schema</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <schemaPackage>com.example.accounts.json</schemaPackage>
</configuration>
```

Gradle:

```kotlin
asyncApiGenerator {
    executions {
        register("json") {
            generatorName.set("json-schema")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            schemaPackage.set("com.example.accounts.json")
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name json-schema \
  --schema-package com.example.accounts.json
```

`schemaPackage` is required and selects the output directory, for example
`com/example/accounts/json/AccountEvent.schema.json`. Maven and Gradle add the
output directory to the project resources.

## What is generated

- Each AsyncAPI Schema Object under `components.schemas`, and each native
  JSON Schema payload (`schemaFormat`
  `application/schema+json;version=draft-07` or
  `application/schema+yaml;version=draft-07`), becomes one
  `<Name>.schema.json` file.
- A reference to another component schema becomes a relative reference to
  that schema's file, such as `"$ref": "AccountStatus.schema.json"`, so keep
  the generated files together.
- Two sources that would produce the same file name, such as a Schema Object
  and a native JSON Schema with the same name, fail generation.

The generator does not promise coverage of every JSON Schema keyword. A
keyword that cannot be represented fails generation instead of being dropped.
The complete approved output for a sample contract is protected by
[`JsonSchemaApprovalTest`](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/kotlin/dev/banking/asyncapi/generator/core/generator/jsonschema/JsonSchemaApprovalTest.kt).
