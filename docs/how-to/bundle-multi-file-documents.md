# Bundle multi-file documents

A contract can be split across files with relative `$ref`s, for example
shared schemas in `common.yaml`. Bundling writes one self-contained AsyncAPI
document that tools without multi-file support, such as schema registries or
documentation portals, can consume.

## Write a bundled document

Select `asyncapi-yaml` or `asyncapi-json` and set `outputFile`.

Maven:

```xml
<configuration>
    <generatorName>asyncapi-yaml</generatorName>
    <inputSpec>${project.basedir}/src/main/asyncapi/main.yaml</inputSpec>
    <outputFile>${project.build.directory}/asyncapi/accounts.yaml</outputFile>
</configuration>
```

Gradle:

```kotlin
asyncApiGenerator {
    executions {
        register("bundle") {
            generatorName.set("asyncapi-yaml")
            inputSpec.set(file("src/main/asyncapi/main.yaml"))
            outputFile.set(layout.buildDirectory.file("asyncapi/accounts.yaml"))
        }
    }
}
```

CLI:

```sh
asyncapi-generator \
  --input-spec src/main/asyncapi/main.yaml \
  --generator-name asyncapi-yaml \
  --output-file target/asyncapi/accounts.yaml
```

A `kotlin`, `java`, or schema execution can write the bundle as well: add
`outputFile` next to its packages and it also writes a YAML bundle. Keep
`outputFile` outside the generated package directories. The bundled file is
not part of the [stale-file cleanup](../reference/generator-configuration.md#stale-generated-files),
so it is replaced when its content changes and is never deleted.

## What the bundle contains

The bundle is built from the validated contract, so a contract that fails
validation is never bundled.

- Objects referenced from other files, such as channels, messages, or
  property schemas, are copied to the place that references them.
- References to component schemas of the root document keep their `$ref`, for
  example `$ref: '#/components/schemas/Account'`.
- A component schema from another file, for example
  `common.yaml#/components/schemas/Money`, is added to the bundle's
  `components.schemas` under its own name, `Money`, and every reference to it
  becomes `#/components/schemas/Money`. Recursive schemas from other files are
  added the same way, so the bundle stays finite.
- Only referenced content is included. Unrelated content of another file is
  not imported.

Shared types are often copied into several files. When two files define a
component schema with the same name and identical content, the bundle
contains it once. When the content differs, bundling fails and names both
origins, because one name cannot describe two types. Rename one schema or make
the copies identical.

Code generated from a bundle is the same as code generated from the original
files: the bundle keeps the schema names that generated types are named after.

## Formatting of the output

The bundle is serialized from the parsed contract, not copied as text.
Comments, anchors, and the original quoting and collection style are not
preserved. See [YAML output](../reference/yaml-output.md) for the exact rules.

References must be local files: relative paths or `file:` URIs. HTTP and other
remote references are not supported; see
[Reference behavior](../reference/parser-contract.md#reference-behavior).
