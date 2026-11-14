# Generate Kotlin and Java models

This guide generates payload model source from the AsyncAPI Schema Objects of
a contract: Kotlin data classes, Java classes, or Java records. For models from
native Avro or Protobuf schemas, see
[Generate Avro schemas](generate-avro-schemas.md) and
[Generate Protobuf schemas and models](generate-protobuf-schemas.md).

## Configure the execution

Select the `kotlin` or `java` generator and set `modelPackage`.

Maven:

```xml
<configuration>
    <generatorName>kotlin</generatorName>
    <inputSpec>${project.basedir}/src/main/resources/asyncapi.yaml</inputSpec>
    <modelPackage>com.example.payments.model</modelPackage>
</configuration>
```

Gradle:

```kotlin
asyncApiGenerator {
    executions {
        register("models") {
            generatorName.set("kotlin")
            inputSpec.set(file("src/main/resources/asyncapi.yaml"))
            modelPackage.set("com.example.payments.model")
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input-spec src/main/resources/asyncapi.yaml \
  --generator-name kotlin \
  --model-package com.example.payments.model
```

The model type defaults to `kotlin-data-class` for `kotlin` and `java-class`
for `java`. Select Java records with `modelConfig.modelType` `java-record`
(CLI `--model-type java-record`). To put an annotation of your own on every
generated model, set `modelConfig.modelAnnotation` (CLI `--model-annotation`)
to a fully qualified annotation type.

## Which schemas become models

The generator writes one file per named component schema and per message
payload, plus a model for every inline object or enum schema it finds inside
them. An inline schema is named after its `title`, or else after the property
that holds it; the inline item schema of a named array schema `Lines` is named
`LinesItem`. A named schema that is only a scalar or an array, such as
`Tags: {type: array, items: {type: string}}`, gets no file of its own; a
reference to it uses its type in place, here `List<String>`. Inline schemas
that describe the same type share one model. Two
inline schemas that would get the same name but describe different types fail
generation, naming both locations; give one a `title` or move it to
`components.schemas`.

## How schemas map to code

| Schema                                   | Kotlin                                                                         | Java                                                              |
|------------------------------------------|--------------------------------------------------------------------------------|-------------------------------------------------------------------|
| Object with properties                   | `data class`                                                                   | Class with getters, setters, and constructors, or a record        |
| Required property                        | Non-null constructor parameter                                                 | Field annotated with `@NotNull`                                   |
| Optional property                        | Nullable parameter with default `null`, or the schema `default`                | Nullable field; schema defaults are not applied                   |
| String enum                              | `enum class`                                                                   | `enum`                                                            |
| `oneOf` or `anyOf` of object schemas     | `sealed interface` implemented by the member classes                           | `interface` implemented by the member classes                     |
| Object without properties                | `typealias Name = Any`; properties of that type are `Any`                      | No file; properties of that type are `Object`                     |
| `additionalProperties` schema            | `Map<String, T>`                                                               | `Map<String, T>`                                                  |
| Array                                    | `List<T>`                                                                      | `List<T>`                                                         |
| `string` with `format` `uuid`, `date`, `date-time`, `time` | `UUID`, `LocalDate`, `OffsetDateTime`, `LocalTime`         | Same Java types                                                   |
| `integer`                                | `Int`, or `Long` for `format: int64` or bounds outside the `Int` range         | `Integer` or `Long`                                               |
| `number`                                 | `Double`; `Float` for `format: float`; `BigDecimal` when `multipleOf` is set   | `Double`, `Float`, or `BigDecimal`                                |

Constraints such as `minLength`, `pattern`, `minimum`, and `format: email` map
to Jakarta Validation annotations where supported, and nested models are
annotated for cascaded validation. The application decides when to validate.

### Wire format

The generated code keeps the JSON wire format of the contract when it is
serialized with Jackson:

- Enum values that are not valid constant names are turned into constants and
  annotated with `@JsonProperty("<value>")`. For example `in-progress` becomes
  `IN_PROGRESS` and `2fa` becomes `_2FA`; both serialize to their contract
  values. Values that already match their constant name are not annotated.
- Properties whose names are not valid identifiers, such as `account-id`, get
  a valid identifier (`accountId`) and a `@JsonProperty` annotation with the
  contract name.
- `readOnly` and `writeOnly` properties are annotated with
  `@JsonProperty(access = Access.READ_ONLY)` or `Access.WRITE_ONLY`.
- A `oneOf` or `anyOf` with a `discriminator` is annotated with
  `@JsonTypeInfo` on the discriminator property and `@JsonSubTypes` listing the
  members. Each member's type name is the `const`, or else the first `enum`
  value, of its discriminator property; without one, it is the member's schema
  name.

### Defaults

Kotlin applies schema `default` values as constructor parameter defaults,
escaped and typed for the property, for example `val retries: Int? = 3` or
`val status: AccountStatus? = AccountStatus.OPEN`. Java models ignore schema
defaults; a property that is absent from the payload is `null`.

### Documentation

Schema `description` values become KDoc or Javadoc. A `*/` sequence in a
description is escaped so it cannot end the comment early.

## Add the runtime dependencies

The generated sources import, depending on the contract:

- `jakarta.validation:jakarta.validation-api` for constraint annotations;
- `com.fasterxml.jackson.core:jackson-annotations` for `@JsonProperty`,
  `@JsonTypeInfo`, and `@JsonSubTypes`.

Add both to the compile classpath. The generated models do not configure a
JSON mapper. Kotlin data classes need a Jackson setup that can construct
Kotlin classes, such as `jackson-module-kotlin`, and date and time types need
`jackson-datatype-jsr310`; Spring Boot registers both when they are on the
classpath.

## See complete examples

The [generated output examples](../reference/generated-output-examples.md)
link fixture contracts to complete approved Kotlin and Java models.
