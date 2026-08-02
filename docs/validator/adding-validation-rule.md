# Add or change a validation rule

Use this procedure when adding a semantic check or changing an existing one.

## 1. Establish ownership and authority

Decide whether the condition is specification conformance, a generator capability limitation, or advisory guidance. For specification rules, identify the exact AsyncAPI 3.0, JSON Schema Draft 7, or official binding section before writing code. Do not turn optional guidance into a conformance error.

## 2. Add stable metadata

Add one entry to `ValidationRule` with a unique code, concern, severity, and documentation link. Use these namespaces:

- `AAS3-` for AsyncAPI 3.0 rules;
- `JSONSCHEMA-` for JSON Schema Draft 7 rules;
- `GEN-` for generator capability limits;
- `ADV-` for advisory rules.

Codes describe a stable condition, not a Kotlin method or message sentence.

## 3. Emit the finding at its owner

Pass the rule and the most specific available `SourceLocation` to the collector:

```kotlin
results.error(
    MESSAGE_CONTENT_TYPE_FORMAT,
    "$contextString has an invalid content type '${message.contentType}'.",
    asyncApiContext.getSourceLocation(message, message::contentType),
)
```

Validate the exact parsed value. Do not trim, unquote, coerce, or otherwise sanitize it in the validator. If structural interpretation is impossible, that belongs to parsing rather than semantic validation.

## 4. Add focused tests

Add at least one valid and one invalid case. For the invalid case, assert the stable rule code, expected severity, source file, nested parser path, and line. Assert message fragments only when wording itself is part of a compatibility contract.

For recursive objects and references, also cover the relevant inline, referenced, external-source, and cycle behavior. Expected invalid input must produce findings rather than implementation exceptions.

## 5. Update the inventory

Add or update the row in [rules.md](rules.md). Record its authority, concern, severity, source field, test owner, and audit status. A rule is not complete until its documentation and tests agree with its code metadata.

## 6. Verify the boundary

Run focused validator tests while developing. Before declaring the slice ready, run:

```shell
mvn -q clean install -pl asyncapi-generator-core
git diff --check
```

Review the branch diff and confirm that parser behavior, external fragment loading, and generated-output approvals did not change unintentionally.
