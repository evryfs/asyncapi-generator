# Bundled document examples

This reference collects complete input and output examples for the bundler's
supported reference behavior. Each approved output is produced through the
reader, parser, validator, bundler, and YAML writer, then parsed and validated
again as a standalone document.

## Example catalog

| Scenario | Source document | Bundled output | Demonstrated behavior |
| --- | --- | --- | --- |
| Transitive multi-file messaging contract | [Source](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/bundler/multi/asyncapi_multifile_example_main.yaml) | [Output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/bundler/multi-file-messaging-contract.approved.yaml) | External servers, channels, messages, schemas, properties, and tags are traversed across several files and serialized without source-file references. |
| Reference-only operation topology | [Source](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/bundler/approval/topology-preserving-operation/main.yaml) | [Output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/bundler/topology-preserving-operation.approved.yaml) | External message and schema content becomes self-contained while channel server references and operation channel and message references remain Reference Objects. |
| Selected foreign schema fragment | [Source](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/validator/schemas/external/asyncapi_external_selected_schema.yaml) | [Output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/bundler/foreign-schema-fragment.approved.yaml) | A compatible schema selected from an OpenAPI document is bundled without copying unrelated OpenAPI paths or schemas. |
| Mutually recursive external schemas | [Source](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/bundler/recursive-external-mutual/asyncapi.yaml) | [Output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/bundler/mutually-recursive-schemas.approved.yaml) | Recursive schemas are promoted to root components and their cycle edges are rewritten as local schema references. |
| External component catalog | [Source](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/bundler/approval/external-component-catalog/main.yaml) | [Output](https://github.com/evryfs/asyncapi-generator/blob/main/asyncapi-generator-core/src/test/resources/approvals/bundler/external-component-catalog.approved.yaml) | Reusable messages, traits, replies, security schemes, correlation IDs, tags, external documentation, and bindings are serialized into the bundled component catalog. |

The source link identifies the root input. Some scenarios deliberately use
additional files beside that root; follow their relative `$ref` values to see
the complete source layout.

## Reference behavior

Bundling makes supported external content available in one portable document,
but it does not replace every Reference Object with an inline object. AsyncAPI
fields whose value is required to be a Reference Object retain that shape. The
examples include these representative paths:

```yaml
channels:
  orders:
    servers:
      - $ref: "#/servers/kafka"
operations:
  sendOrder:
    channel:
      $ref: "#/channels/orders"
    messages:
      - $ref: "#/channels/orders/messages/orderCreated"
```

Ordinary reusable objects can be serialized inline after their external target
has been traversed. The external component catalog shows this for nested traits,
reply data, security schemes, correlation IDs, tags, and external documentation,
while its operation channel still remains a local reference.

Schema cycles use a separate policy. A recursive external schema cannot be
expanded indefinitely, so the bundler promotes the cycle members into
`components.schemas` and uses local references between them:

```yaml
components:
  schemas:
    ParentNode:
      properties:
        child:
          $ref: "#/components/schemas/ChildNode"
    ChildNode:
      properties:
        parent:
          $ref: "#/components/schemas/ParentNode"
```

These examples document supported production workflows rather than exhaustive
coverage of every AsyncAPI or JSON Schema reference location. The
[bundler strategy](bundler_strategy.md) defines the boundary and its intentional
limits.
