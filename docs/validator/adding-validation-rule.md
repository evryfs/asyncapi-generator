# Add or change a validation rule

Use this procedure when adding a semantic check or changing an existing one.

## 1. Establish ownership and authority

Decide whether the condition is specification conformance, a generator capability limitation, or advisory guidance. For specification rules, identify the exact AsyncAPI 3.0, JSON Schema Draft 7, or official binding section before writing code. Do not promote optional guidance to errors.

## 2. Add stable metadata

Add one entry to `ValidationRule` with a unique code, concern, severity, and documentation link. Use these namespaces:

- `AAS3-` for AsyncAPI 3.0 rules
- `JSONSCHEMA-` for JSON Schema Draft 7 rules
- `GEN-` for generator capability limits
- `ADV-` for advisory guidance

Rule metadata should describe a stable condition, not a Kotlin method or message sentence.

## 3. Emit the finding at its owner

Pass the rule and the most specific available `SourceLocation` to the collector:

```kotlin
results.error(
    MESSAGE_CONTENT_TYPE_FORMAT,
    "$contextString has an invalid content type '${message.contentType}'.",
    asyncApiContext.getSourceLocation(message, message::contentType),
)
```

Validate the exact parsed value. Do not trim, unquote, coerce, or otherwise sanitize it in the validator. If a failure is truly structural, keep that check in parser mapping and model construction.

## 4. Add focused tests with visible assertions

Add at least one valid and one invalid case. For invalid cases, assert:

- `finding.code`
- `finding.concern`
- `finding.severity`
- `finding.path`
- relevant `sourceLocation.file` and `line` where source ownership matters

Message fragments are assertion-worthy only when wording itself is part of a compatibility contract.

For recursive objects and references, cover representative inline/reference/external/cycle behavior that can change outcome. Do not require full combinatorial matrix coverage unless each dimension carries distinct semantic risk.

## 5. Update the inventory

Add or update the row in [rules.md](rules.md) with the rule code, condition, authority, and concern/severity. The documentation and tests must agree on behavior.

## 6. Verify the boundary

Run focused validator tests while developing. Before declaring the work ready, run:

```shell
mvn -q clean install -pl asyncapi-generator-core
git diff --check
```

Review the branch diff and confirm that parser behavior, external fragment loading, and generated-output approvals did not change unintentionally.
