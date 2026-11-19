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
REFERENCE_CYCLE("AAS3-REFERENCE-CYCLE", SPECIFICATION, ERROR, spec("referenceObject")),
```

- The code is stable public identity; never reuse or rename a published code.
  After the first release, retired codes stay in the enum and the inventory
  as legacy identifiers.
- The prefix states the authority: `AAS3-` for the AsyncAPI 3.0
  specification, `JSONSCHEMA-` for JSON Schema Draft 7, `GEN-` for generator
  capability limits, and `ADV-` for project advisories.
  The enum name is the code without its prefix (`JSONSCHEMA-` codes keep a
  `SCHEMA_` prefix). `ValidationRuleTest` enforces unique codes, these
  prefixes, and the names.
- Choose the concern (`SPECIFICATION`, `GENERATOR_CAPABILITY`, or `ADVISORY`)
  and the severity. Errors stop the pipeline; warnings are reported and
  generation continues.
- The rule owns its documentation link, and every finding of the rule uses it.
  `AAS3-` rules pass the anchored specification section (`spec("...")` or
  `kafkaBinding("...")`), and `JSONSCHEMA-` rules the keyword page
  (`jsonSchema("...")`). `GEN-` and `ADV-` rules pass nothing: they link to
  their row in `docs/reference/validation-rules.md`, which needs an
  `<a id="<code in lower case>"></a>` anchor.

## 2. Report the finding

Report the finding from the rule set that owns the object (`ChannelRules`,
`MessageRules`, …), under
`asyncapi-generator-core/src/main/kotlin/dev/banking/asyncapi/generator/core/validator/`.
Start the message with the object's `ValidationPath` and pass the source
location of the offending field so users see the file, line, and snippet:

```kotlin
walk.results.error(
    LICENSE_NAME_REQUIRED,
    "$path 'name' is required and cannot be empty.",
    asyncApiContext.modelTracking.getSourceLocation(node, node::name),
)
```

Use `warn(...)` for warning rules. Prefer the shared helpers in
`validator/util` (`requireNonBlank`, `requireAbsoluteUri`,
`EnumConstrainedValueRules`) when a rule has their shape. A rule that relates
objects across the whole document is a `DocumentRule`, and a rule for one
protocol's bindings belongs to its `ProtocolBindingRules`. Rule sets never
change the model, resolve references, or build other rule sets; children are
checked through the `ValidationWalk`. See the
[validator architecture](../explanation/validator.md).

## 3. Test it

Add a fixture under
`asyncapi-generator-core/src/test/resources/validator/<area>/` and a case to
the validator test for that area, such as `InfoValidatorTest`, which validates
the fixture through `AsyncApiValidator`. Assert the
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
