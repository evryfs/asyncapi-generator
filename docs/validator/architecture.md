# Validator architecture

## Responsibility

Validation starts after reading and parsing. It consumes an `AsyncApiDocument` and checks semantic rules that cannot be guaranteed by the parsed Kotlin model alone. It does not reopen source files, reinterpret YAML or JSON syntax, normalize values, resolve documents by reading them, bundle models, or generate output.

The reader owns syntax and source coordinates. The parser owns structural interpretation and domain construction. External loading owns locating and parsing referenced documents or fragments. The validator owns semantic conformance, generator capability limits, advisories, and reference integrity over the resulting model graph.

## Data flow

```text
AsyncApiDocument
    -> AsyncApiValidator
    -> AsyncApiValidationProfile
    -> domain validators and reference resolution
    -> ValidationCollector (invocation-local mutable state)
    -> ValidationReport (immutable public result)
    -> ValidationReporter or frontend-specific rendering
```

`AsyncApiValidator` selects the validation profile once from the parsed specification version. Validators receive the collector selected for that invocation; they do not compare raw version strings.

`ValidationCollector` is mutable only while one validation is running. Each finding is created from a catalogued `ValidationRule`. `ValidationReport` snapshots the findings and exposes read-only `findings`, `errors`, and `warnings` lists. Logging and exception construction happen after validation through `ValidationReporter`, except where a frontend intentionally controls its own output channel.

The collector also owns invocation-local identity sets and a queue of resolved reference targets. Domain validators record each reference edge with its concrete expected category. Resolution follows reference-to-reference chains, checks the final target category, and queues reachable targets. `AsyncApiValidator` drains that queue through explicit category dispatch. A model instance is entered once, so shared targets do not duplicate findings and reference cycles terminate without recursion.

Semantic format checks operate on the exact strings stored in the domain model. Validators do not trim, unquote, or otherwise normalize values. URI syntax uses `java.net.URI`, media types use Jakarta Activation, email addresses use Jakarta Mail, and runtime-expression fragments use Jackson's JSON Pointer parser.

## Finding contract

Every finding contains:

- a stable `code` suitable for tests and tooling;
- a `concern`: `SPECIFICATION`, `GENERATOR_CAPABILITY`, or `ADVISORY`;
- a `severity`;
- human-readable `message` text;
- one `SourceLocation`, when the parsed value owns a location;
- an authoritative or explanatory documentation link.

The parser path, file, line, and column belong to `SourceLocation`. `ValidationFinding.path` and `ValidationFinding.line` are derived conveniences; they are not independently mutable coordinates.

Messages remain useful to people but are not stable machine identifiers. Tests should normally assert the rule code, severity, and source location rather than freeze complete prose.

## Rule ownership

`SPECIFICATION` findings enforce AsyncAPI 3.0, the supported JSON Schema dialect, or an official protocol binding. `GENERATOR_CAPABILITY` findings describe otherwise valid input that this generator cannot safely consume. `ADVISORY` findings are optional guidance and must not be presented as specification conformance.

The [rule inventory](rules.md) records the present implementation, including legacy rules that require correction in later hardening slices. Assigning a stable code in this first slice does not endorse a legacy rule as correct.

## External fragments

An external AsyncAPI document is parsed and validated as a document. A fragment selected from a non-AsyncAPI document is parsed and validated only as the requested AsyncAPI object category. Unrelated surrounding OpenAPI, JSON Schema, or plain YAML content is outside the validator boundary.

External fragment validation currently uses the AsyncAPI 3.0 profile because that is the only parser/validator profile implemented. Later traversal work will centralize validation of the complete reachable graph without changing this fragment boundary.
