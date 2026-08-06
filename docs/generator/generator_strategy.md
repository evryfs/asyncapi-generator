# Generator Strategy

This document outlines the architectural strategy for the code generation phase of the `asyncapi-generator-core`.

## Core Architecture

The generator follows an **Orchestrator Pattern**. The main `AsyncApiGenerator` class orchestrates the flow but 
delegates specific tasks to specialized components.

### The Pipeline

1.  **Prepare Input:** The parsed `AsyncApiDocument` is converted into `GenerationInput`. JSON-compatible AsyncAPI Schema Object payloads are kept separately from explicit multi-format schemas.
2.  **Plan:** The typed generator configuration is converted into a `GenerationPlan` with explicit output tasks.
3.  **Validate Compatibility:** The planned outputs are checked against the prepared input before any files are written.
4.  **Analyze:** Schema and channel analyzers build generation-focused models such as relationships, payload names, topic addresses, message payload contracts, and Kafka key and header bindings.
5.  **Generate:** Specialized generators render source and schema artifacts from the prepared input and planned tasks.
6.  **Write:** Generated artifacts are written through the output contract into either source or resource output directories.

---

## Generation and Output Boundary

Planning and compatibility validation complete before rendering starts. A planned capability that is not implemented must fail explicitly during compatibility validation; it must not be logged and skipped, because a successful run otherwise implies that every planned output was produced.

Specialized generators own model preparation and template rendering. They return `GenerationResult` values containing relative paths, content, and artifact kinds, but do not create directories or write files. `AsyncApiGenerator` is the orchestration boundary that passes those results to the configured `GeneratedArtifactWriter`. This keeps destination selection and filesystem behavior out of language, client, and schema renderers.

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

---

## Payload Format Boundary

The generator intentionally separates AsyncAPI Schema Object payloads from native or explicit multi-format payload schemas.

`GenerationInput.schemas` contains component schemas that can be consumed as JSON-compatible AsyncAPI Schema Object payloads. These are the schemas used by Kotlin model generation, Java model generation, Spring Kafka client generation, and Avro Projection.

`GenerationInput.multiFormatSchemas` contains component schemas declared with a known `schemaFormat`, such as native Avro or Protobuf. These schemas are preserved so dedicated generator capabilities can consume them without losing their original format.

Channel analysis follows the same boundary. `AnalyzedChannel.messages` contains messages with AsyncAPI Schema Object payloads. `AnalyzedChannel.multiFormatMessages` contains messages with explicit multi-format payloads.

Model generation and Avro Projection reject multi-format payloads through `GenerationInputCompatibilityValidator`. This is deliberate. Those outputs consume AsyncAPI Schema Object payloads only, and native Avro or Protobuf support is modeled as dedicated generator capabilities instead of silently projecting one transfer format into another.

Spring Kafka client generation can reference native Avro `SpecificRecord` payload types and native Protobuf Java message payload types when those schemas provide enough native package and type information. The native schema artifact generators are still responsible for producing the `.avsc`, `.proto`, `SpecificRecord`, or Java Protobuf message source artifacts.

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

### 1. Enum Handling: Strict Support
Contrary to earlier iterations or loose mapping strategies, this generator **fully supports Avro Enums**.

*   **Mapping:** AsyncAPI schemas with `type: string` and `enum: [...]` are generated as first-class Avro `enum` types.
*   **Defaults:** Supports the Avro `default` property for Enums (crucial for schema evolution).
*   **Naming:** Anonymous enums are automatically named based on their parent property path to satisfy Avro's nominal type requirement.

### 2. Record Handling
*   **Objects:** AsyncAPI `type: object` maps to Avro `record`.
*   **Nullable Fields:** Handled via Avro Unions `["null", "Type"]`.
*   **Logical Types:** Supports `decimal`, `uuid`, `date`, `time-millis`, `timestamp-millis`.

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

## Protobuf Generator Strategy

Native Protobuf generation consumes schemas declared through Protobuf `schemaFormat`. It writes native `.proto` schema artifacts and can generate Java Protobuf message sources by running `protoc` during generation.

Spring Kafka client generation can reference those generated Java message types when the `.proto` schema declares a Java package or Protobuf package, enables `option java_multiple_files = true;`, and contains a top-level message matching the AsyncAPI payload name.

## Future Enhancements

*  **Common Type Mapping:** We have some redundancy in type mapping logic across generators. Future work will focus on 
centralizing this logic where possible. We also need to consider more languages, which is an argument for granular type
mapping strategies.
