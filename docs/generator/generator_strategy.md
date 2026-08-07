# Generator Strategy

This document outlines the architectural strategy for the code generation phase of the `asyncapi-generator-core`.

## Core Architecture

The generator follows an **Orchestrator Pattern**. The main `AsyncApiGenerator` class orchestrates the flow but 
delegates specific tasks to specialized components.

### The Pipeline

1.  **Prepare Input:** Load, normalize, and analyze the parsed `AsyncApiDocument` into `GenerationInput`. JSON-compatible AsyncAPI Schema Object payloads remain separate from explicit multi-format schemas, while generation-focused relationships, payload names, topic addresses, Kafka keys, and message headers are derived.
2.  **Plan:** Convert the typed generator configuration into a `GenerationPlan` with explicit output tasks.
3.  **Validate Compatibility:** Check that every planned output can consume the prepared input.
4.  **Render:** Render or serialize every planned output in memory.
5.  **Preflight:** Resolve every configured destination and reject collisions before creating output directories or files.
6.  **Write:** Write the complete rendered result to its rooted or explicitly configured destinations.

---

## Generation and Output Boundary

Planning and compatibility validation complete before rendering starts. A planned capability that is not implemented must fail explicitly during compatibility validation; it must not be logged and skipped, because a successful run otherwise implies that every planned output was produced.

Specialized generators own model preparation and template rendering. They return `GenerationResult` values containing rooted artifacts or explicit bundled-document output, but do not create configured output directories or write configured destinations. `AsyncApiGenerator` renders every artifact task before it invokes the configured `GeneratedArtifactWriter`, so a later rendering failure cannot leave outputs from earlier tasks. This keeps destination selection and filesystem behavior out of language, client, and schema renderers.

Before writing, the filesystem writer resolves rooted artifacts against their configured output roots, resolves bundled documents to their explicit files, and rejects destination collisions. This prevents one generated output from silently overwriting another, including when source and Java-source roots point to the same directory.

Bundled document output retains the explicit file configured for that task rather than being routed through a source or resource output root. Document serialization remains owned by `DocumentArtifactGeneration` and `AsyncApiRegistry`, but its rendered content and explicit destination participate in the same render-before-write and collision-preflight boundary as other generated output.

Compatibility checks that depend only on prepared input and the generation plan belong in `GenerationInputCompatibilityValidator`. Renderers may still reject invalid states discovered while building their generation-specific models, but they do not repeat compatibility checks already completed before output begins.

---

## Client Contract Selection

Spring Kafka producer and consumer contract selection belongs to the typed generator configuration. When client
generation is active, each enabled contract type is generated for every channel that declares at least one message.
All messages declared by that channel contribute methods to the generated interface.

AsyncAPI Operation Objects are not generation directives. Their `send` and `receive` actions continue through the
reader, parser, validator, and bundler, but channel analysis does not use them to activate, suppress, or filter client
contracts. This keeps generation deterministic for both application-oriented documents and channel-oriented
integration contracts.

### Spring Kafka Planning and Rendering

Spring Kafka contract generation prepares one language-neutral contract for each analyzed channel. Preparation owns
the generated contract and method names, topic-property placeholders, payload identity, supported native payload
types, Kafka key contracts, header contracts, and canonical producer payload variants. Compatibility validation uses
the same producer method planning when detecting generated-name collisions.

Java and Kotlin generation consume that prepared contract but retain ownership of source-language syntax, imports,
nullability, validation annotation placement, parameter formatting, and generated documentation. Producer and consumer
templates render those language-specific models; they do not repeat channel or message analysis. Unsupported client
input must fail compatibility validation or contract preparation rather than being omitted during rendering.

### Producer Payload Representations

Every payload-bearing Spring Kafka producer contract includes the contract-derived `send<MessageName>` method. This
method is the stable expression of the AsyncAPI payload schema and retains configured payload validation annotations.
The producer `additionalPayloadTypes` collection may add `byte-array` and `string` representations for payloads that
the application has already serialized. Omitted or empty configuration generates only the contract-derived method.
Configured alternatives generate `send<MessageName>ByteArray` and `send<MessageName>String` after that method; they do
not replace it and do not receive the payload validation annotation.

The additional values must already conform to the AsyncAPI payload schema and content type. Serializer selection,
message conversion, encoding, record construction, and compatible Spring Kafka configuration remain application-owned.
In particular, implementations must not pass already serialized values through a JSON converter that serializes them
again. Consumer payload contracts are unaffected by producer representation configuration.

---

## Payload Format Boundary

The generator intentionally separates AsyncAPI Schema Object payloads from native or explicit multi-format payload schemas.

`GenerationInput.schemas` contains component schemas that can be consumed as JSON-compatible AsyncAPI Schema Object payloads. These are the schemas used by Kotlin model generation, Java model generation, Spring Kafka client generation, and Avro Projection.

`GenerationInput.multiFormatSchemas` contains component schemas declared with a known `schemaFormat`, such as native Avro or Protobuf. These schemas are preserved so dedicated generator capabilities can consume them without losing their original format.

Channel analysis follows the same boundary. `AnalyzedChannel.messages` contains messages with AsyncAPI Schema Object payloads. `AnalyzedChannel.multiFormatMessages` contains messages with explicit multi-format payloads.

Model generation rejects multi-format payloads through `GenerationInputCompatibilityValidator`. Avro Projection also
never interprets a multi-format schema as an AsyncAPI Schema Object. Multi-format schemas may coexist with projection
only when the generation plan contains the matching native Avro or Protobuf capability; otherwise compatibility
validation rejects the unhandled format before rendering.

Spring Kafka client generation can reference native Avro `SpecificRecord` payload types and native Protobuf Java message payload types when those schemas provide enough native package and type information. The native schema artifact generators are still responsible for producing the `.avsc`, `.proto`, `SpecificRecord`, or Java Protobuf message source artifacts.

Schema artifact generators render component schemas in component-name order. This keeps output and collision
diagnostics deterministic when declarations are reordered in the source document.

---

## Schema Normalization Boundary

Source-model and Avro projection generators consume a static view of AsyncAPI Schema Objects. `GenerationInput.schemas`
contains that normalized view, while `GenerationInput.declaredSchemas` retains the parsed declarations for outputs that
must preserve schema semantics, including standalone JSON Schema generation.

Normalization flattens supported `allOf` compositions by following resolved schema references and merging their object
properties, required members, generated types, validation constraints, array items, map values, and polymorphic model
metadata. Recursive compositions terminate by schema identity, so external schemas with the same component name are
not mistaken for cycles.

Conditional `if`/`then`/`else` schemas cannot be represented as runtime branching in a static generated type.
Normalization therefore produces a property superset: unconditional properties retain their declared shape,
branch-only properties remain optional, and incompatible branch shapes become schemas without a fixed JSON type. Java
and Kotlin models render those open values as `Object` and `Any`; other generators retain ownership of their open-schema
mapping. When both branches use the same generated type but declare different constraints, those branch-specific
constraints are not applied unconditionally. Applications that require complete conditional validation must validate
against the declared schema rather than relying on the generated type alone.

---

## Language Generators

### Kotlin & Java Generators
Both generators share a similar structure:
*   **Model Factory:** Converts internal `Schema` objects into rich `GeneratorItem` models (Data Class, Enum, Interface).
*   **Class Generator:** Maps the rich model to a `Map<String, Any?>` context for Mustache.
*   **Template Engine:** Renders the final source code.

**Key Features:**
*   **Data Classes/POJOs/Records:** Full support for properties, types, and nullability.
*   **Enums:** Generation of strict Enum classes.
*   **Polymorphism:** `oneOf`/`anyOf` are mapped to Sealed Interfaces (Kotlin) or Interface hierarchies (Java).
*   **Validation:** `jakarta.validation` annotations are added automatically.

---

## Avro Generator Strategy

The current Avro generator is an **Avro Projection** generator. It produces `.avsc` files from JSON-compatible AsyncAPI Schema Object definitions.

It does not consume native Avro schemas declared through `schemaFormat`, and it does not generate Avro `SpecificRecord` classes. Native Avro support is implemented as a separate generator capability so users can choose between:

*   AsyncAPI Schema Object -> Java/Kotlin payload models.
*   AsyncAPI Schema Object -> projected `.avsc` files.
*   Native Avro schemaFormat -> native Avro artifacts and generated `SpecificRecord` classes.

Projection emits named record and enum artifacts. A top-level schema composed only from referenced `oneOf` alternatives
does not produce a separate union file; its referenced concrete record or enum schemas are generated independently.
Field-level Avro unions remain supported for nullable fields and supported `oneOf` property schemas.

### 1. Enum Handling: Strict Support
Contrary to earlier iterations or loose mapping strategies, this generator **fully supports Avro Enums**.

*   **Mapping:** AsyncAPI schemas with `type: string` and `enum: [...]` are generated as first-class Avro `enum` types.
*   **Defaults:** Supports the Avro `default` property for Enums (crucial for schema evolution).
*   **Naming:** Anonymous enums are automatically named based on their parent property path to satisfy Avro's nominal type requirement.

### 2. Record Handling
*   **Objects:** AsyncAPI `type: object` maps to Avro `record`.
*   **Nullable Fields:** Handled via Avro Unions `["null", "Type"]`.
*   **Logical Types:** Supports `uuid`, `date`, and `timestamp-millis` mappings.

### 3. Evolution Strategy
While Avro Enums are supported, users are advised to use the `default` property for enum symbols to ensure forward compatibility (allowing readers to handle unknown symbols safely).

```json
{
  "type": "enum",
  "name": "Status",
  "symbols": ["OPEN", "CLOSED", "UNKNOWN"],
  "default": "UNKNOWN" 
}
```

---

## JSON Schema Generator Strategy

Standalone JSON Schema generation consumes AsyncAPI Schema Objects and native Draft 7 schemas. AsyncAPI-only Schema
Object fields are removed, an explicit Draft 7 `$schema` declaration is added when absent, and component references are
rewritten to the corresponding generated `.schema.json` files. Boolean schemas and explicitly declared null defaults
or constants retain their exact Draft 7 meaning.

---

## Protobuf Generator Strategy

Native Protobuf generation consumes schemas declared through Protobuf `schemaFormat`. It writes native `.proto` schema
artifacts and can run `protoc` to generate Java message sources or Java message sources with Kotlin DSL APIs.

Spring Kafka client generation can reference those generated Java message types when the `.proto` schema declares a Java package or Protobuf package, enables `option java_multiple_files = true;`, and contains a top-level message matching the AsyncAPI payload name.
