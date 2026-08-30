# Troubleshoot generation

Generation failures identify the pipeline boundary that rejected the input,
configuration, requested target, or destination. Start with the first reported
failure and use the matching section below.

## Fix an unreadable input path or unsupported extension

Every frontend requires `inputSpec` to identify a readable file. If the path is
missing, names a directory, does not exist, or cannot be read, correct the path
in the Maven execution, Gradle execution, or CLI invocation.

The file extension selects the reader. Use `.yaml`, `.yml`, or `.json`; other
extensions are rejected even when their content is YAML or JSON. If the reader
reports malformed UTF-8 or a resource limit, correct the file encoding or
reduce the document within the documented [reader limits](../reference/reader-contract.md#resource-limits).

Frontend field names and defaults are listed in
[Generator configuration](../reference/generator-configuration.md#common-frontend-fields).

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

## Correct incompatible configuration or no output

Configuration can fail before the input document is read. Compare the reported
field with the [required combinations and diagnostics](../reference/generator-configuration.md#required-combinations-and-diagnostics).
In particular:

- choose one supported `generatorName`;
- configure `modelPackage` for model output;
- configure both package fields and complete Spring Kafka client settings for
  client output;
- configure `schemaPackage` for a schema profile; or
- configure `outputFile` for `asyncapi-yaml` or `asyncapi-json`.

`No generator output is configured` means that the selected profile did not
have an activating package or output file. `Generation completed without
producing any artifacts` means the request was valid but the contract did not
produce an artifact for it. Compare the requested output with the supported
input described in the [capability matrix](../reference/supported-capabilities.md).

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
