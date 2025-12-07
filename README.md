# asyncapi-generator

This project is a modular AsyncAPI generator written in Kotlin, designed to parse AsyncAPI specifications and generate type-safe code.

# Features supported

## Parsing Core
The core parsing logic is stable and handles the structural validation of AsyncAPI documents.

- [x] **YAML Support:** Reads and Parses yaml format.
- [x] **Context-Aware Error Reporting:** Provides precise error messages with line numbers and JSON paths.
- [x] **$ref Resolution:** Supports internal and external file references.
- [x] **Components:** Full parsing support for Schemas, Messages, Channels, Parameters, etc.

## Schema Formats
- [x] **AsyncAPI Schema:** Fully supported (default format).
- [ ] **Multi-Format Schemas (Avro, Protobuf, etc.):**
    - *Parsing:* The `MultiFormatSchemaParser` successfully extracts the raw schema content and format type.
    - *Generation:* **Not Implemented.** The generator currently does not possess the logic to translate non-AsyncAPI 
schemas (like Avro `.avsc` or Protobuf `.proto`) into corresponding POJOs or classes. These schemas are currently 
treated as opaque data and will throw a 'UnsupportedException' if generation is attempted.

## Generator Capabilities

- [x] Kotlin Code Generation
- [x] Spring Kafka Kotlin Generation
- [ ] Quarkus Kafka Kotlin Generation

- [x] Java Code Generation
- [x] Spring Kafka Java Generation
- [ ] Quarkus Kafka Java Generation

- [x] Avro Schema Generation

