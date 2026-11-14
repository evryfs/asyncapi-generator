# Troubleshoot generation

Generation failures identify the pipeline boundary that rejected the input,
configuration, requested target, or destination. Start with the first reported
failure and use the matching section below.

Every frontend checks the input file first. Maven and the CLI then check the
generator configuration, then the contract (reading, parsing, validation, and
bundling), then target compatibility, and finally the output destinations.
Gradle reads the contract while configuring the build, to find every file it
references, so in Gradle a contract error can appear before a configuration
error.

## Fix a missing input document

The input file is checked before anything else, so a wrong path is never hidden
behind a configuration error. Maven and Gradle report
`Input document does not exist: <absolute path>`; the CLI rejects the
`--input-spec` value with a usage error that names the file and says it does
not exist. A directory or unreadable file is rejected in the same way.

Correct `inputSpec` in the Maven execution or Gradle execution, or
`--input-spec` on the command line.

## Fix an unsupported extension or malformed document

The file extension selects the reader. Use `.yaml`, `.yml`, or `.json`; other
extensions are rejected even when their content is YAML or JSON. If the reader
reports malformed UTF-8 or a resource limit, correct the file encoding or
reduce the document within the documented
[reader limits](../reference/reader-contract.md#resource-limits).

A YAML alias that contains itself, such as an anchor `&node` whose content
refers back to `*node`, describes an infinite document. It is reported as
`Malformed input document ... (Recursive YAML alias at <path>: the aliased node
contains itself)`. Replace the alias with a `$ref` to a named component schema;
recursive schemas are supported through references.

## Correct configuration errors

Configuration is checked after the input file and before generation. Compare
the reported field with the
[required combinations and diagnostics](../reference/generator-configuration.md#required-combinations-and-diagnostics).
In particular:

- choose one supported `generatorName`;
- configure `modelPackage` for model output;
- configure both package fields and complete Spring Kafka client settings for
  client output;
- configure `schemaPackage` for a schema profile; or
- configure `outputFile` for `asyncapi-yaml` or `asyncapi-json`.

Common messages:

| Message                                                      | Cause and fix                                                                                                                                                                                         |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `No generator output is configured`                          | The selected profile has no activating package or output file. Add `modelPackage`, `clientPackage` with client settings, `schemaPackage` for a schema profile, or `outputFile`.                        |
| `clientConfig is required when clientPackage is configured`  | `clientPackage` is set but there are no client settings. Add `clientConfig` (Maven and Gradle) or `--client-type spring-kafka --client-contract interface` (CLI), or remove `clientPackage`.            |
| `modelPackage is required when clientPackage is configured`  | Client contracts refer to the payload models, so the execution must also generate them. Add `modelPackage` to the same execution.                                                                   |
| `Invalid suppressedWarnings '<id>'. Supported values: ...`   | A suppressed warning id is misspelled or unknown. Use one of the listed ids.                                                                                                                          |
| `Generation completed without producing any artifacts.`      | The request was valid but the contract produced no artifact for it. Compare the requested output with the supported input in the [capability matrix](../reference/supported-capabilities.md). |

## Silence or act on an unused client configuration warning

`clientConfig is ignored because clientPackage is not configured, so no client
is generated` is a warning, not a failure. It appears when client settings
reach an execution that does not set `clientPackage`. This is common when
client settings are shared: Gradle's `clientConfig` applies to every
execution, and Maven plugin-level configuration is inherited by every
execution.

If the execution should generate clients, add `clientPackage` (and
`modelPackage`). If it intentionally generates only models, schemas, or a
document, silence the warning with the id `unused-client-config`:

- Maven: `<suppressedWarnings><warning>unused-client-config</warning></suppressedWarnings>`
  in the plugin or execution configuration.
- Gradle: `suppressedWarnings.add("unused-client-config")` on
  `asyncApiGenerator` or on one execution.
- CLI: `--suppress-warning unused-client-config`.

## Use a parser diagnostic and source location

A parser diagnostic reports a code, document path, source file, line, and
column. Fix the value at that location; do not infer the location from the
exception text alone. For an external reference failure, the location points
to the `$ref` that could not be parsed or resolved.

Check that required members are present, values use the reported runtime type,
and the root declares a supported AsyncAPI `3.0.x` version. External references
must use internal pointers, relative local paths, or `file:` URIs. HTTP and
other remote schemes are unsupported.

The [parser diagnostic reference](../reference/parser-contract.md#parser-diagnostics)
lists the current codes and the
[reference behavior](../reference/parser-contract.md#reference-behavior)
explains local path and JSON Pointer handling.

## Resolve validation findings

Validation errors report contract-wide specification, reference-integrity, or
supported-workflow violations and stop generation. Warnings do not stop
generation, but should be reviewed because they identify advisories or
generator-wide capability limits. These findings are distinct from the
target-specific compatibility failures described below.

Use the finding code to locate its condition, authority, and severity in the
[validation rule inventory](../reference/validation-rules.md). Correct the
contract at the finding's source path and location. Findings from loaded
external documents retain the external source location, so change that source
rather than the root file when appropriate.

`AAS3-REFERENCE-CYCLE` means a chain of `$ref`s points only at other `$ref`s
and returns to its start, for example a message that references a message that
references the first one. The message prints the chain. Point one of the
references at an actual object. A schema whose properties refer back to the
schema itself is a recursive schema, not a reference cycle, and is supported.

## Resolve an inline schema name collision

Inline object and enum schemas get a generated type name: their `title`, or
else the name of the property that holds them. When two inline schemas would
get the same name but describe different types, generation fails with a
message such as:

```text
Inline schema at components.schemas.Supplier.properties.address would be
generated as 'Address', which is already used by the different schema at
components.schemas.Customer.properties.address.
```

Give one of the schemas a distinct `title`, or define the type once under
`components.schemas` and reference it from both places. Inline schemas with
the same type shape share one generated type; differences in `description`,
`default`, or `const` alone do not cause a collision.

## Resolve a bundling name collision

When a referenced file defines a component schema with the same name as a
schema already in the bundle but with different content, bundling fails and
prints both origins (`Existing schema` and `Incoming schema`). One name cannot
describe two types. Rename one of the schemas, or make the copies identical if
they are meant to be the same type. Identical copies are reused. See
[Bundle multi-file documents](bundle-multi-file-documents.md).

## Resolve a target compatibility failure

A target compatibility failure occurs after the contract has parsed and
validated successfully but the selected output cannot represent part of it.
The contract is not coerced or stripped to make generation continue.

Use the exception's contract path or schema name to identify the incompatible
input. Then either select an output that supports that input or deliberately
change the contract or target configuration. Common target requirements, such
as native Avro and Protobuf package matching, are documented under
[required combinations](../reference/generator-configuration.md#required-combinations-and-diagnostics).
The [capability matrix](../reference/supported-capabilities.md) records broader
target limitations.

An Avro failure such as `enum symbol 'in-progress' in
components.schemas.PaymentStatus is not a valid Avro name` means a property
name or enum value does not match `[A-Za-z_][A-Za-z0-9_]*`. The generator does
not rename it, because a renamed field or symbol would no longer match data
or schemas already registered under the contract's name. Rename it in the
contract, or do not generate Avro for that schema. See
[Generate Avro schemas](generate-avro-schemas.md).

## Resolve a protoc resolution failure

Native Protobuf models (`modelType` `protobuf-message`) run `protoc`, which is
resolved as the artifact `com.google.protobuf:protoc` the first time it is
needed. If resolution fails, the build has no access to a repository that
provides it:

- Maven resolves it through the project's repositories, so `settings.xml`
  mirrors, proxies, and credentials apply. Make the artifact available in your
  repository manager.
- Gradle resolves it from the project's `repositories`. Add a repository that
  provides it, for example `mavenCentral()` or your mirror.
- The CLI reads it from the local Maven repository (`~/.m2/repository`, or
  `maven.repo.local`) or downloads it from Maven Central using the JVM proxy
  settings.

On machines without repository access, point the generator at a local
executable: Maven `protocExecutable`, Gradle `protocExecutable`, or CLI
`--protoc`. See [Generate Protobuf schemas and models](generate-protobuf-schemas.md).

## Resolve an output destination conflict

An existing `outputDirectory` must be a directory, and `outputFile` must not be
a directory. Correct either path when the frontend reports one of these
conditions.

If multiple artifacts resolve to the same normalized destination, generation
reports the destination and each colliding artifact before writing. Move
`outputFile` outside the generated package path, or change the relevant output
directory or package so every artifact has a distinct destination.

If staging or committing a file fails, use the artifact and destination in the
message to correct the unwritable or conflicting filesystem path. Output path
semantics and frontend defaults are documented under
[Output directories](../reference/generator-configuration.md#output-directories).

## Understand removed or remaining generated files

Maven and Gradle remove a generated file when the execution that generated it
no longer generates it, for example after a schema is removed from the
contract. They never remove files they did not generate, such as hand-written
sources in the same directory. The CLI never removes files. See
[Stale generated files](../reference/generator-configuration.md#stale-generated-files)
for the exact rules.
