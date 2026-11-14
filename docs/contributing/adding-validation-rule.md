# Adding a validation rule

Use this procedure for a new contract-wide finding reported by the semantic
validator. Before adding a rule, determine which validator it belongs to; see
[Validation boundary](validation-boundary.md). A restriction that applies only
to one requested output belongs in generation compatibility validation and
does not get a rule code.

## 1. Declare the rule

Add an entry to `ValidationRule` in
`asyncapi-generator-core/src/main/kotlin/dev/banking/asyncapi/generator/core/model/validator/ValidationRule.kt`,
next to related rules:

```kotlin
REFERENCE_CYCLE("AAS3-REFERENCE-CYCLE", SPECIFICATION, ERROR, ASYNCAPI_SPEC),
```

- The code is stable public identity; never reuse or rename a published code.
  Retired codes stay in the enum and the inventory as legacy identifiers.
- The prefix states the authority: `AAS3-` for the AsyncAPI 3.0
  specification, `JSONSCHEMA-` for JSON Schema Draft 7, `GEN-` for generator
  capability limits, and `ADV-` for project advisories.
  `ValidationRuleTest` enforces unique codes and these prefixes.
- Choose the concern (`SPECIFICATION`, `GENERATOR_CAPABILITY`, or `ADVISORY`)
  and the severity. Errors stop the pipeline; warnings are reported and
  generation continues.
- Point the documentation link at the specification section, or at the
  project's validation rule inventory for project rules.

## 2. Report the finding

Report the finding from the validator that owns the object, under
`asyncapi-generator-core/src/main/kotlin/dev/banking/asyncapi/generator/core/validator/`.
Pass the source location of the offending field so users see the file, line,
and snippet:

```kotlin
results.error(
    LICENSE_NAME_REQUIRED,
    "$contextString 'name' field is required and cannot be empty.",
    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(node, node::name),
)
```

Use `results.warn(...)` for warning rules. Validators do not modify the
model.

## 3. Test it

Add a fixture under
`asyncapi-generator-core/src/test/resources/validator/<area>/` and a case to
the validator test for that area, such as `InfoValidatorTest`. Assert the
finding code, severity, path, and source line, not the message text. Keep one
focused failing case and rely on the existing valid fixtures for the passing
case.

If the rule changes what users observe end to end, add an `invalidContract`
diagnostic scenario to `asyncapi-generator-system-test` as described in
[Development setup](development-setup.md#adding-a-scenario).

## 4. Document it

Add a row to the [validation rule inventory](../reference/validation-rules.md)
in the same change, with the code, the condition in one sentence, the
authority, and the concern and severity. When users need guidance to fix the
finding, add it to [Troubleshooting](../how-to/troubleshooting.md).
