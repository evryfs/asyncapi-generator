# Generator Strategy

This document outlines the architectural strategy for the code generation phase of the `asyncapi-generator-core`.

## Core Architecture

The generator follows an **Orchestrator Pattern**. The main `AsyncApiGenerator` class orchestrates the flow but 
delegates specific tasks to specialized components.

### The Pipeline

1.  **Load & Normalize:** The raw `AsyncApiDocument` is processed to resolve inline schemas and normalize types.
2.  **Analyze:** The `SchemaAnalyzer` scans the document to build a graph of relationships (inheritance, polymorphism, dependencies).
3.  **Contextualize:** A `GeneratorContext` is built, holding the "Rich Models" ready for generation.
4.  **Generate:** Specialized Generators (Kotlin, Java, Avro) iterate over the models and produce files using Mustache templates.

---

## Language Generators

### Kotlin & Java Generators
Both generators share a similar structure:
*   **Model Factory:** Converts internal `Schema` objects into rich `GeneratorItem` models (Data Class, Enum, Interface).
*   **Class Generator:** Maps the rich model to a `Map<String, Any?>` context for Mustache.
*   **Template Engine:** Renders the final source code.

**Key Features:**
*   **Data Classes/POJOs:** Full support for properties, types, and nullability.
*   **Enums:** Generation of strict Enum classes.
*   **Polymorphism:** `oneOf`/`anyOf` are mapped to Sealed Interfaces (Kotlin) or Interface hierarchies (Java).
*   **Validation:** `jakarta.validation` annotations are added automatically.

---

## Avro Generator Strategy

The Avro generator produces `.avsc` (Avro Schema) files from the AsyncAPI definition.

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
