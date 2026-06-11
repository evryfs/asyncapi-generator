# Generator Strategy

This document outlines the architectural strategy for the code generation phase of the `asyncapi-generator-core`.

## Core Architecture

The generator follows an **Orchestrator Pattern**. The main `AsyncApiGenerator` class orchestrates the flow but 
delegates specific tasks to specialized components.

### The Pipeline

1.  **Prepare Input:** The parsed `AsyncApiDocument` is converted into `GenerationInput`. JSON-compatible AsyncAPI Schema Object payloads are kept separately from explicit multi-format schemas.
2.  **Plan:** The typed generator configuration is converted into a `GenerationPlan` with explicit output tasks.
3.  **Validate Compatibility:** The planned outputs are checked against the prepared input before any files are written.
4.  **Analyze:** Schema and channel analyzers build generation-focused models such as relationships, payload names, channel directions, and message payload contracts.
5.  **Generate:** Specialized generators render source and schema artifacts from the prepared input and planned tasks.
6.  **Write:** Generated artifacts are written through the output contract into either source or resource output directories.

---

## Payload Format Boundary

The generator intentionally separates AsyncAPI Schema Object payloads from native or explicit multi-format payload schemas.

`GenerationInput.schemas` contains component schemas that can be consumed as JSON-compatible AsyncAPI Schema Object payloads. These are the schemas used by Kotlin model generation, Java model generation, Spring Kafka client generation, and Avro Projection.

`GenerationInput.multiFormatSchemas` contains component schemas declared with a known `schemaFormat`, such as native Avro or Protobuf. These schemas are preserved so dedicated future generator capabilities can consume them without losing their original format.

Channel analysis follows the same boundary. `AnalyzedChannel.messages` contains messages with AsyncAPI Schema Object payloads. `AnalyzedChannel.multiFormatMessages` contains messages with explicit multi-format payloads.

Existing model, Spring Kafka, and Avro Projection outputs reject multi-format payloads through `GenerationInputCompatibilityValidator`. This is deliberate. These outputs currently support AsyncAPI Schema Object payloads only, and native Avro or Protobuf support should be modeled as dedicated generator capabilities instead of silently projecting one transfer format into another.

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

It does not consume native Avro schemas declared through `schemaFormat`, and it does not generate Avro `SpecificRecord` classes. Native Avro support should be implemented as a separate generator capability so users can choose between:

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
This pattern is fully supported by the generator.

## Future Enhancements

*  **Common Type Mapping:** We have some redundancy in type mapping logic across generators. Future work will focus on 
centralizing this logic where possible. We also need to consider more languages, which is an argument for granular type
mapping strategies.
