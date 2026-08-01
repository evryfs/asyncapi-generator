# Change the parser safely

Use this guide when adding an AsyncAPI member, changing an existing domain
parser, extending Schema Object support, or changing reader and diagnostic
behavior.

## 1. Place the change in one stage

Start by classifying the behavior.

| Change | Owner |
| --- | --- |
| YAML/JSON syntax, duplicate keys, scalar interpretation, source marks | `reader` |
| Cursor traversal, runtime type expectations, parser paths | `parser.node` |
| AsyncAPI object members and domain-model construction | The matching domain parser package |
| Schema Object keywords or `schemaFormat` dispatch | `parser.schemas` |
| External path, URI, JSON Pointer, document identity, or fragment selection | `context` external-reference classes |
| Cross-field, specification, or reference-resolution rule on a model | `validator` |
| Inlining or rewriting references | `bundler` |

If a change appears to need logic in several stages, first identify the smallest
contract each stage needs. Do not let domain parsers inspect SnakeYAML/Jackson
types, let ordinary parsers open files, or move semantic validation into
`ParserNode`.

## 2. State the behavior change

Write down the before and after behavior before editing. Include valid YAML and
JSON behavior, absent versus explicit null, the rejected runtime type, the
expected parser path and source location, and whether external references are
involved.

Malformed-input behavior may become stricter, but valid supported documents and
their domain models must remain stable. A stricter change needs a focused
negative test that demonstrates why the former behavior was not part of the
supported AsyncAPI contract.

## 3. Use the narrow parser operation

For a required scalar member:

```kotlin
val name = node.required("name").expect<String>()
```

For an optional scalar member:

```kotlin
val description = node.optional("description")?.expect<String>()
```

For a typed collection, express the full nested type:

```kotlin
val tags = node.optional("tags")?.expect<List<String>>()
```

For a domain object or map, use `members()` and delegate each child to the
appropriate parser. For an array of domain objects, use `elements()`. These
operations reject the wrong container shape and preserve child paths.

Use `toPlainValue()` only when the specification intentionally accepts any
JSON-compatible value. Do not use it to avoid defining a known field's type.

Register every constructed domain object with the node that owns it:

```kotlin
return Model(...).also { asyncApiContext.register(it, node) }
```

For a reference, assign the concrete `ReferenceCategoryKey` and register the
`Reference` at the Reference Object node so source ownership and external
loading remain correct.

## 4. Treat schemas separately

Before parsing a Schema Object keyword with a generic expectation, decide
whether it can be a boolean schema, reference, recursive schema, Multi Format
Schema, or free-form JSON value. Reuse `SchemaParser.parseElement` for recursive
schema positions.

Keep structural dispatch in `SchemaParser` and keyword policy in
`SchemaValidator`. Preserve the distinction between absent values and explicit
null when the model exposes that distinction.

## 5. Add focused tests

Add tests beside the owning implementation. Cover the smallest relevant set:

- valid input and the resulting domain value;
- a missing required member;
- explicit null when it differs from absence;
- a wrong scalar or container type;
- an invalid nested list or map element when generics are involved;
- parser diagnostic category, expected type, actual type/value, path, file,
  line, and column when diagnostic behavior changes;
- equivalent YAML and JSON when reader behavior is involved; and
- relative path, same-named files, encoded paths, escaped JSON Pointer tokens,
  missing documents/targets, and cycles when external loading is involved.

Schema changes should cover boolean, reference, inline, and Multi Format Schema
branches that the keyword can affect. Free-form tests should include nested
objects, arrays, scalars, and explicit null as applicable.

Avoid fixtures that rely on non-standard shapes. For example, AsyncAPI 3
`Channel.messages` is a map; a list of Reference Objects is malformed even if a
previous parser happened to synthesize map keys for it.

## 6. Verify the slice

Run the focused test class or package while implementing. Before handing off a
parser slice, run:

```shell
mvn -q clean install -pl asyncapi-generator-core
git diff --check
```

Review the complete diff against the branch base and confirm that it contains
only the intended parser work. At the pull-request-ready stage, also run the
complete repository build when parser behavior affects Maven invoker fixtures
or other module integrations.

## 7. Review the boundary

Before declaring the change complete, confirm all of the following:

- YAML and JSON reach the same format-independent node contract.
- A known field is not accepted through unsafe coercion or an unchecked generic
  cast.
- Absence and explicit null have deliberate behavior.
- Failures point to the originating source and parser path.
- References retain their concrete category and source owner.
- External loading has not leaked into an ordinary domain parser.
- Schema-specific shapes still use `SchemaParser`.
- Extensions and free-form fields are the only places that discard node typing
  through `toPlainValue()`.
- Validation, bundling, and generation responsibilities remain downstream.
- Existing valid contracts, external fragments, Native/Multi Format Schema
  behavior, bundled output, and generated output remain stable.
