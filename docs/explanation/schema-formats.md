# Schema formats

An AsyncAPI message payload is described either by an AsyncAPI Schema Object
or by a native schema in another format. The choice decides which outputs the
generator can produce, because the generator never converts one native format
into another.

## AsyncAPI Schema Objects

A payload without `schemaFormat`, or with an AsyncAPI `schemaFormat` such as
`application/vnd.aai.asyncapi+json;version=3.0.0`, is an AsyncAPI Schema
Object: a superset of JSON Schema Draft 7. It is the contract format the
generator understands in depth. From it the generator produces:

- Kotlin and Java models, whose JSON wire format follows the schema;
- Spring Kafka client interfaces that use those models;
- JSON Schema files (`json-schema`); and
- projected Avro schemas (`avro-schema`).

Avro Projection translates the schema structure into Avro. It is a derived
artifact: Avro has stricter naming rules and its own encoding, so a projection
only succeeds when every name in the schema is also a valid Avro name. The
generator fails rather than rename, because a renamed field would not match
the JSON payloads the contract describes.

## Native Avro

A Multi Format Schema Object with an Avro `schemaFormat`
(`application/vnd.apache.avro;version=1.9.0`, or its `+json` and `+yaml`
variants) carries an Avro schema as written. The generator writes it unchanged
as an `.avsc` file and can generate Java SpecificRecord classes from it with
`modelType` `avro-specific-record`. Choose native Avro when Avro is the wire
format and the Avro schema is the source of truth, for example when it is
registered in a schema registry.

## Native Protobuf

A Multi Format Schema Object with `application/vnd.google.protobuf;version=2`
or `version=3` carries a `.proto` definition. The generator writes it
unchanged as a `.proto` file and can compile it with `protoc` into Java
messages, and for Kotlin the official Kotlin DSL, with `modelType`
`protobuf-message`. There is no projection from AsyncAPI Schema Objects to
Protobuf.

## Native JSON Schema

A Multi Format Schema Object with
`application/schema+json;version=draft-07` or
`application/schema+yaml;version=draft-07` carries a JSON Schema Draft 7
document. The `json-schema` generator writes it as a schema file.

## Summary

| Payload description     | Kotlin / Java models | Spring Kafka clients | `avro-schema`   | `protobuf-schema` | `json-schema` | Native runtime models      |
|-------------------------|----------------------|----------------------|-----------------|-------------------|---------------|----------------------------|
| AsyncAPI Schema Object  | Yes                  | Yes                  | Projected       | No                | Yes           | No                         |
| Native Avro             | No                   | Yes                  | Copied          | No                | No            | `avro-specific-record`     |
| Native Protobuf         | No                   | Yes                  | No              | Copied            | No            | `protobuf-message`         |
| Native JSON Schema      | No                   | No                   | No              | No                | Yes           | No                         |

The generator does not skip payloads it cannot handle. When a contract
contains a payload format that the selected output cannot represent, such as
a native Avro payload in a `json-schema` execution, generation fails and names
the payload and its `schemaFormat`. Other `schemaFormat` values that the
parser recognizes, such as OpenAPI or RAML schemas, can only be bundled
(`asyncapi-yaml`, `asyncapi-json`). See
[Supported capabilities](../reference/supported-capabilities.md) for limits
and [Generator configuration](../reference/generator-configuration.md) for
the settings of each output.
