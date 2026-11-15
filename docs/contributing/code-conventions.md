# Code conventions

These conventions apply to every Kotlin source in the core, CLI, Maven plugin,
and Gradle plugin modules. ktlint and detekt check the mechanical parts in
every build. The design parts are checked in review.

## Design principles

The code follows a small set of domain-driven design principles. It does not
adopt the full pattern catalogue: there are no aggregates, domain events, or
repositories per entity.

1. **One owner per phase.** Each pipeline phase has one responsibility, a
   typed input, and a typed output. A phase never changes the output of
   another phase, and nothing happens as a hidden side effect of a call that
   is named for something else.
2. **One vocabulary.** Package and type names come from the AsyncAPI
   specification and from the pipeline phases. A term means the same thing
   everywhere. Names such as `util`, `helper`, `manager`, `common`, or an
   `Interface` suffix do not describe a domain concept and are not used.
3. **Value objects instead of primitives.** Identities, paths, type
   references, and names that carry rules are types, not strings.
4. **Immutable models.** Data passed between phases is immutable. Mutable
   state stays private to the phase that owns it.
5. **Dependencies point downward.** A phase depends only on the shared kernel
   and on the phases before it. Translation to and from the outside world
   (frontend configuration, Avro, Protobuf, Jackson, the file system) happens
   at the edges.

## Bounded contexts

Core is organised by bounded context. Each context is a package under
`dev.banking.asyncapi.generator.core`.

| Context | Responsibility |
|---------|----------------|
| Source | Reads YAML or JSON into a document tree with source locations, within resource limits. |
| Parsing | Turns the document tree into the AsyncAPI model, loads external documents, and links references. |
| Validation | Applies read-only rules to the linked model and reports findings. |
| Bundling | Turns the linked model into one self-contained document. |
| Generation: configuration | Turns a frontend request into a typed generator configuration. |
| Generation: planning | Turns the configuration into generation tasks. |
| Generation: schema catalog | Loads, normalises, and analyses the schemas that generation uses. |
| Generation: messaging | Describes channels and messages (topics, payloads, keys, headers) in terms of the type model. Protocol-specific knowledge such as Kafka lives here. |
| Generation: type model | Describes generated types independently of any target language. |
| Generation: targets | Renders one output format each: Kotlin, Java, Avro, Protobuf, JSON Schema, and documents. |
| Generation: output | Writes artifacts and removes stale generated files. |

The shared kernel holds what every context uses: the AsyncAPI model,
diagnostics (findings, warnings, exceptions, and their layout), and source
identity (source ids, node paths, source locations).

The root package `dev.banking.asyncapi.generator.core` holds only what
frontends call: the generator entry point and its configuration types.

## Visibility

Core is an implementation module of the CLI and the build plugins. It is
published so that the plugins can resolve it, but it is not a supported
library and has no compatibility promise between versions.

- Declarations in core are `internal` unless a frontend uses them.
- A frontend uses core only through the root package.
- Inside a context, helpers are `private` to their file or class.

## Size and shape

| Limit | Value | Checked by |
|-------|-------|------------|
| Class length | 250 lines | detekt `LargeClass` |
| Function length | 30 lines | detekt `LongMethod` |
| Line length | 120 characters | ktlint and detekt `MaxLineLength` |
| Function parameters | 6 (constructors 8), parameters with defaults excluded | detekt `LongParameterList` |

- One top-level public or internal type per file, named after the file. A
  sealed type and its subtypes may share a file.
- A class with several unrelated responsibilities is split, even when it is
  under the size limit.
- Tests are exempt from the size limits.

## Kotlin style

ktlint enforces formatting with the `ktlint_official` code style
(`.editorconfig`). In addition:

- Use exhaustive `when` over sealed types and enums. Do not add an `else`
  branch that hides a missing case.
- Use named arguments when a call passes several values of the same type, or
  any boolean literal.
- Do not use `!!`. Express the invariant with a type, or use `requireNotNull`
  or `checkNotNull` with a message.
- Use `require` for invalid arguments and `check` for invalid state. Failures
  that the user can fix are diagnostics, not `require` or `check`.
- Use scope functions (`let`, `also`, `apply`, `run`, `with`) only when they
  make the code shorter and clearer, never to chain unrelated steps.
- Prefer expression bodies for functions that are one expression.
- No wildcard imports.

## Errors and diagnostics

- A problem in the user's contract or configuration is reported as a
  diagnostic with a rule id, a location when one exists, and a documentation
  link. Errors stop the run; warnings are collected and reported.
- A failed assumption in the code is a programming error and uses `require`,
  `check`, or `error`.
- Exceptions are not used for control flow between phases.

## Documentation comments

KDoc follows the [KDoc style guide](kdoc-style-guide.md).

## Tests

- Core tests stay lean: one test per behaviour, no overlapping coverage.
- The system tests own end-to-end coverage. Existing scenarios are not
  changed when fixing bugs; new coverage goes into new scenarios.
- Tests assert behaviour, not incidental values.

## Tooling

| Command | What it does |
|---------|--------------|
| `mvn ktlint:format` | Formats the Kotlin sources. |
| `mvn ktlint:check` | Fails on formatting violations. It also runs in `mvn verify`. |
| `mvn verify` | Runs detekt, which fails on any finding that is not in the baseline. |

Each module has a `detekt-baseline.xml` that lists the findings that existed
when detekt was introduced. The baseline only shrinks:

- Never add an entry to a baseline. Fix the finding, or, if the rule is wrong
  for that case, suppress it in code with `@Suppress` and a comment that says
  why.
- A baseline entry is keyed by the function signature. When a refactoring
  changes the signature of a function that already has an entry, update the
  key in the same change; that is not a new finding.
- When a cleanup fixes a baselined finding, remove its entry. Regenerate the
  baseline with `mvn detekt:create-baseline -pl <module>` and check that the
  diff only removes lines.
