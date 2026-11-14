# Generate Avro schemas

The generator produces Avro in two ways:

- **Avro Projection**: `.avsc` files derived from the AsyncAPI Schema Objects
  of the contract, with the `avro-schema` generator.
- **Native Avro**: Avro schemas embedded in, or referenced from, the contract
  as Multi Format Schema Objects. The `avro-schema` generator writes them as
  `.avsc` files, and the `avro-specific-record` model type also generates Java
  SpecificRecord classes from them.

[Schema formats](../explanation/schema-formats.md) explains when to choose
which.

## Generate projected Avro schemas

Select `avro-schema` and set `schemaPackage`. The package is the Avro
`namespace` of the generated schemas and the directory they are written to.

Maven:

```xml
<configuration>
    <generatorName>avro-schema</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <schemaPackage>com.example.accounts.avro</schemaPackage>
</configuration>
```

Gradle:

```kotlin
asyncApiGenerator {
    executions {
        register("avro") {
            generatorName.set("avro-schema")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            schemaPackage.set("com.example.accounts.avro")
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name avro-schema \
  --schema-package com.example.accounts.avro
```

Each object and enum schema becomes one file, for example
`com/example/accounts/avro/AccountEvent.avsc`. Maven and Gradle add the output
directory to the project resources, so the files are packaged with the
application.

### How schemas are projected

| AsyncAPI Schema Object                     | Avro                                                                                       |
|--------------------------------------------|--------------------------------------------------------------------------------------------|
| Object with properties                     | `record`, one field per property                                                           |
| String enum                                | `enum`; a schema `default` becomes the enum default                                        |
| Reference to another schema                | The full name of that schema, such as `com.example.accounts.avro.AccountStatus`            |
| `string`                                   | `string`; `format` `uuid` adds logical type `uuid`                                         |
| `string` with `format` `date` / `date-time` | `int` with logical type `date` / `long` with logical type `timestamp-millis`             |
| `integer`                                  | `int`, or `long` for `format: int64`                                                       |
| `number`                                   | `double`, for every `format`                                                               |
| `boolean`                                  | `boolean`                                                                                  |
| `array`                                    | `array`, with item types mapped recursively, including arrays of arrays                    |
| Object with `additionalProperties`         | `map`, with value types mapped recursively                                                 |
| `oneOf`                                    | A union of the member types                                                                |
| `description`                              | `doc`, JSON-escaped                                                                        |

Optional properties become unions with `null`:

- Without a default, the field type is `["null", T]` with `"default": null`.
- With a scalar or enum default, the field type is `[T, "null"]` with that
  default, because Avro requires a union's default to match its first branch.
- An optional `oneOf` becomes one flat union with `null` first, because Avro
  unions cannot contain unions.

### Names must already be valid Avro names

Avro names must match `[A-Za-z_][A-Za-z0-9_]*`. A property such as
`account-id` or an enum value such as `in-progress` fails generation with a
message naming the schema and the value. The generator does not rename them:
a renamed field or symbol would no longer match the contract's JSON wire
format or schemas already registered in a schema registry. Rename the value
in the contract, or keep Avro out of the generation for that contract.

## Generate native Avro schemas and SpecificRecords

Embed the Avro schema in the contract, or reference an `.avsc` file next to
it:

```yaml
components:
  schemas:
    UserCreated:
      schemaFormat: application/vnd.apache.avro+json;version=1.9.0
      schema:
        $ref: user-created.avsc
```

The accepted formats are `application/vnd.apache.avro;version=1.9.0`,
`application/vnd.apache.avro+json;version=1.9.0`, and
`application/vnd.apache.avro+yaml;version=1.9.0`.

With `avro-schema`, the native schema is written as an `.avsc` file. To also
generate Java SpecificRecord classes, use a `java` or `kotlin` execution with
model type `avro-specific-record`:

```xml
<configuration>
    <generatorName>java</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <modelPackage>com.example.avro</modelPackage>
    <modelConfig>
        <modelType>avro-specific-record</modelType>
    </modelConfig>
</configuration>
```

In Gradle set `modelConfig { modelType.set("avro-specific-record") }`; in the
CLI pass `--model-type avro-specific-record`. `modelPackage` must equal the
`namespace` of every native Avro payload schema, and the schema must declare
one. The generated classes are Java source even with the `kotlin` generator,
and the application needs `org.apache.avro:avro` on the classpath. An
optional `schemaPackage` changes the directory of the written `.avsc` files,
not the namespace inside them.

A complete example is linked from
[Native Avro schema and SpecificRecord](../reference/generated-output-examples.md#native-avro-schema-and-specificrecord),
and a projected one from
[Avro Projection from an AsyncAPI Schema Object](../reference/generated-output-examples.md#avro-projection-from-an-asyncapi-schema-object).
