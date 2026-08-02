# Validation rule inventory

This reference inventories the checks present when validator hardening began. The stable code and metadata live in `ValidationRule`; this matrix records why each check exists and whether its current implementation should be preserved, corrected, or expanded.

Statuses mean: **valid** is presently defensible, **incomplete** has a sound purpose but incomplete coverage or overly broad implementation, **incorrect** conflicts with the governing specification, **advisory** is optional guidance, and **generator-specific** is a capability boundary rather than conformance.

Authorities are [AsyncAPI 3.0 (A3)](https://www.asyncapi.com/docs/reference/specification/v3.0.0), [the AsyncAPI 3.0 Schema Object (A3 Schema)](https://www.asyncapi.com/docs/reference/specification/v3.0.0#schemaObject), and [JSON Schema Draft 7 validation (JS7)](https://json-schema.org/draft-07/draft-handrews-json-schema-validation-01). Test names identify the focused test owner; gaps are explicit.

## Document and metadata

| Code | Object and condition | Authority | Concern / severity | Source field | Status | Test |
|---|---|---|---|---|---|---|
| `AAS3-DOCUMENT-ID-FORMAT` | Document `id` is not a URI | A3 Identifier | specification / error | `id` | valid | `AsyncApiValidatorTest` |
| `ADV-DOCUMENT-ID-URN` | Document `id` is not a URN | Project guidance | advisory / warning | `id` | advisory | gap: add focused warning test |
| `AAS3-DOCUMENT-CONTENT-TYPE` | `defaultContentType` is not a specific media type | A3 AsyncAPI Object | specification / error | `defaultContentType` | valid | `AsyncApiValidatorTest` |
| `AAS3-INFO-TITLE-REQUIRED` | Info title is empty | A3 Info Object | specification / error | `info.title` | valid | `InfoValidatorTest` |
| `AAS3-INFO-VERSION-REQUIRED` | Application version is empty | A3 Info Object | specification / error | `info.version` | valid | `InfoValidatorTest` |
| `ADV-INFO-VERSION-FORMAT` | Application version is not semver-like | Project guidance | advisory / warning | `info.version` | advisory | `InfoValidatorTest` |
| `AAS3-INFO-TERMS-URI` | Terms-of-service value is not an absolute URI | A3 Info Object | specification / error | `info.termsOfService` | valid | `InfoValidatorTest` |
| `ADV-CONTACT-EMPTY` | Contact has no name, URL, or email | Project guidance | advisory / warning | `info.contact` | advisory | `InfoValidatorTest` |
| `AAS3-CONTACT-URL-FORMAT` | Contact URL is not an absolute URI | A3 Contact Object | specification / error | `contact.url` | valid | `InfoValidatorTest` |
| `AAS3-CONTACT-EMAIL-FORMAT` | Contact email is malformed | A3 Contact Object | specification / error | `contact.email` | incomplete: format implementation needs audit | `InfoValidatorTest` |
| `AAS3-LICENSE-NAME-REQUIRED` | License name is empty | A3 License Object | specification / error | `license.name` | valid | `InfoValidatorTest` |
| `AAS3-LICENSE-URL-FORMAT` | License URL is not an absolute URI | A3 License Object | specification / error | `license.url` | valid | `InfoValidatorTest` |
| `AAS3-EXTERNAL-DOC-URL-REQUIRED` | External documentation URL is empty | A3 External Documentation Object | specification / error | `externalDocs.url` | valid | `ExternalDocsValidatorTest` |
| `AAS3-EXTERNAL-DOC-URL-FORMAT` | External documentation URL is not an absolute URI | A3 External Documentation Object | specification / error | `externalDocs.url` | valid | `ExternalDocsValidatorTest` |
| `AAS3-TAG-NAME-REQUIRED` | Tag name is empty | A3 Tag Object | specification / error | `tag.name` | valid | `TagValidatorTest` |

## Servers, channels, parameters, and correlation IDs

| Code | Object and condition | Authority | Concern / severity | Source field | Status | Test |
|---|---|---|---|---|---|---|
| `AAS3-SERVER-HOST-REQUIRED` | Server host is empty | A3 Server Object | specification / error | `server.host` | valid | `ServerValidatorTest` |
| `AAS3-SERVER-NAME-FORMAT` | Server map key uses unsupported characters | A3 Servers Object | specification / error | server entry | valid | `ServerValidatorTest` |
| `ADV-SERVER-HOST-PROTOCOL` | Host embeds a protocol scheme | Project guidance | advisory / warning | `server.host` | advisory | `ServerValidatorTest` |
| `AAS3-SERVER-VARIABLE-UNDEFINED` | Host or pathname variable lacks a definition | A3 Server Object | specification / error | `server.host` or `server.pathname` | valid | `ServerValidatorTest` |
| `AAS3-SERVER-PROTOCOL-REQUIRED` | Server protocol is empty | A3 Server Object | specification / error | `server.protocol` | valid | `ServerValidatorTest` |
| `AAS3-SERVER-VARIABLE-ENUM-UNIQUE` | Server-variable enum repeats values | A3 Server Variable Object | specification / error | `variable.enum` | valid | `ServerVariableValidatorTest` |
| `ADV-SERVER-VARIABLE-DEFAULT-ENUM` | Default is outside enum | Project guidance | advisory / warning | `variable.default` | advisory | `ServerValidatorTest` |
| `ADV-SERVER-VARIABLE-EXAMPLES-EMPTY` | Examples collection is empty | Project guidance | advisory / warning | `variable.examples` | advisory | `ServerVariableValidatorTest` |
| `ADV-SERVER-VARIABLE-EXAMPLES-ENUM` | Example is outside enum | Project guidance | advisory / warning | `variable.examples` | advisory | `ServerVariableValidatorTest` |
| `AAS3-CHANNEL-ADDRESS-SUFFIX` | Address contains a query or fragment | A3 Channel Object | specification / error | `channel.address` | valid | `ChannelValidatorTest` |
| `AAS3-CHANNEL-PARAMETER-UNDEFINED` | Address parameter lacks a definition | A3 Channel Object | specification / error | `channel.address` | valid | `ChannelValidatorTest` |
| `AAS3-CHANNEL-PARAMETER-UNUSED` | Parameters are present without matching address expressions | A3 Channel and Parameters Objects | specification / error | `channel.parameters` | valid | `ChannelValidatorTest` |
| `ADV-CHANNEL-SERVERS-EMPTY` | Explicit servers list is empty | Project guidance | advisory / warning | `channel.servers` | advisory | `ChannelValidatorTest` |
| `GEN-CHANNEL-MESSAGES-AMBIGUOUS` | Multiple messages exceed a generator path | Generator behavior | capability / warning | `channel.messages` | generator-specific | `ChannelValidatorTest` |
| `ADV-CHANNEL-BINDINGS-EMPTY` | Explicit bindings object is empty | Project guidance | advisory / warning | `channel.bindings` | advisory | `ChannelValidatorTest` |
| `AAS3-PARAMETER-ENUM-UNIQUE` | Parameter enum repeats values | A3 Parameter Object | specification / error | `parameter.enum` | valid | `ParameterValidatorTest` |
| `AAS3-PARAMETER-NAME-FORMAT` | Parameter map key uses unsupported characters | A3 Parameters Object | specification / error | parameter entry | valid | `ParameterValidatorTest` |
| `ADV-PARAMETER-DEFAULT-ENUM` | Parameter default is outside enum | Project guidance | advisory / warning | `parameter.default` | advisory | `ParameterValidatorTest` |
| `ADV-PARAMETER-EXAMPLES-ENUM` | Parameter example is outside enum | Project guidance | advisory / warning | `parameter.examples` | advisory | `ParameterValidatorTest` |
| `AAS3-PARAMETER-LOCATION` | Parameter location expression is malformed | A3 Parameter Object | specification / error | `parameter.location` | valid | `ParameterValidatorTest` |
| `AAS3-CORRELATION-LOCATION-REQUIRED` | Correlation location is empty | A3 Correlation ID Object | specification / error | `correlationId.location` | valid | `CorrelationIdValidatorTest` |
| `AAS3-CORRELATION-LOCATION-FORMAT` | Correlation location expression is malformed | A3 Runtime Expressions | specification / error | `correlationId.location` | valid | `CorrelationIdValidatorTest` |

## Messages, operations, and security

| Code | Object and condition | Authority | Concern / severity | Source field | Status | Test |
|---|---|---|---|---|---|---|
| `ADV-MESSAGE-TRAIT-EMPTY` | Message trait defines no fields | Project guidance | advisory / warning | message trait | advisory | `MessageTraitValidatorTest` |
| `AAS3-MESSAGE-CONTENT-TYPE` | Message or message-trait content type is not a specific media type | A3 Message Object and Message Trait Object | specification / error | `message.contentType` | valid | `MessageValidatorTest`, `MessageTraitValidatorTest` |
| `AAS3-MESSAGE-EXAMPLE-CONTENT` | Message example contains neither headers nor payload | A3 Message Example Object | specification / error | message example | valid; explicit null payload counts as present | `MessageValidatorTest`, `MessageTraitValidatorTest` |
| `GEN-MESSAGE-HEADER-FORMAT` | Header schema form is unsupported | Generator behavior | capability / warning | `message.headers` | generator-specific | `MessageValidatorTest` |
| `AAS3-OPERATION-ACTION-REQUIRED` | Operation action is absent | A3 Operation Object | specification / error | `operation.action` | valid | `OperationValidatorTest` |
| `AAS3-OPERATION-ACTION-VALUE` | Action is not send or receive | A3 Operation Object | specification / error | `operation.action` | valid | `OperationValidatorTest` |
| `AAS3-OPERATION-CHANNEL-REQUIRED` | Operation channel reference is absent | A3 Operation Object | specification / error | `operation.channel` | valid | `OperationValidatorTest` |
| `AAS3-OPERATION-CHANNEL-TARGET` | Operation channel reference resolves to another category | A3 Operation Object | specification / error | `operation.channel.$ref` | valid | `OperationValidatorTest` |
| `AAS3-OPERATION-CHANNEL-SCOPE` | Root operation channel is not owned by root `channels` | A3 Operation Object | specification / error | `operation.channel.$ref` | valid | `OperationValidatorTest` |
| `AAS3-OPERATION-MESSAGE-REFERENCE` | Operation message does not reference an entry in its channel | A3 Operation Object | specification / error | `operation.messages` | valid | `OperationValidatorTest` |
| `ADV-OPERATION-TRAIT-EMPTY` | Operation trait defines no fields | Project guidance | advisory / warning | operation trait | advisory | `OperationValidatorTest` |
| `ADV-OPERATION-REPLY-MESSAGES-EMPTY` | Reply explicitly has no messages | Project guidance | advisory / warning | `reply.messages` | advisory | gap: focused reply test |
| `AAS3-REPLY-CHANNEL-REQUIRED` | Reply defines messages without a channel | A3 Operation Reply Object | specification / error | `reply.messages` | valid | `OperationValidatorTest` |
| `AAS3-REPLY-CHANNEL-SCOPE` | Inline root-operation reply channel is not owned by root `channels` | A3 Operation Reply Object | specification / error | `reply.channel.$ref` | valid | `OperationValidatorTest` |
| `AAS3-REPLY-CHANNEL-ADDRESS` | Reply defines an address while its channel has a known address | A3 Operation Reply Object | specification / error | `reply.channel` | valid | `OperationValidatorTest` |
| `AAS3-REPLY-MESSAGE-REFERENCE` | Reply message does not reference an entry in its reply channel | A3 Operation Reply Object | specification / error | `reply.messages` | valid | `OperationValidatorTest` |
| `AAS3-REPLY-ADDRESS-REQUIRED` | Reply address location is absent | A3 Operation Reply Address | specification / error | `reply.address.location` | valid | gap: focused address error test |
| `AAS3-REPLY-ADDRESS-FORMAT` | Reply address expression is malformed | A3 Runtime Expressions | specification / error | `reply.address.location` | valid | `OperationValidatorTest`, `ReferenceIntegrityValidatorTest` |
| `AAS3-SECURITY-TYPE-REQUIRED` | Security-scheme type is absent | A3 Security Scheme Object | specification / error | `security.type` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-TYPE-VALUE` | Security-scheme type is unsupported | A3 Security Scheme Object | specification / error | `security.type` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-NAME-REQUIRED` | HTTP API-key scheme lacks name | A3 Security Scheme Object | specification / error | `security.name` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-IN-REQUIRED` | API-key scheme lacks its location | A3 Security Scheme Object | specification / error | security scheme | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-IN-VALUE` | API-key location is invalid | A3 Security Scheme Object | specification / error | `security.in` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-SCHEME-REQUIRED` | HTTP scheme lacks scheme | A3 Security Scheme Object | specification / error | `security.scheme` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-OAUTH-FLOWS` | OAuth scheme lacks flows | A3 Security Scheme Object | specification / error | `security.flows` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-AUTHORIZATION-URL-REQUIRED` | OAuth flow lacks its required authorization URL | A3 OAuth Flow Object | specification / error | OAuth flow | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-AUTHORIZATION-URL-FORMAT` | OAuth authorization URL is not absolute | A3 OAuth Flow Object | specification / error | `authorizationUrl` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-TOKEN-URL-REQUIRED` | OAuth flow lacks its required token URL | A3 OAuth Flow Object | specification / error | OAuth flow | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-TOKEN-URL-FORMAT` | OAuth token URL is not absolute | A3 OAuth Flow Object | specification / error | `tokenUrl` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-REFRESH-URL-FORMAT` | OAuth refresh URL is not absolute | A3 OAuth Flow Object | specification / error | `refreshUrl` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-AVAILABLE-SCOPES-REQUIRED` | OAuth flow lacks `availableScopes` | A3 OAuth Flow Object | specification / error | OAuth flow | valid | `SecuritySchemeValidatorTest` |
| `AAS3-OAUTH-SCOPE-AVAILABLE` | Requested OAuth scope is not declared by any configured flow | A3 Security Scheme and OAuth Flow Objects | specification / error | `security.scopes` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-OPENID-URL-REQUIRED` | OpenID Connect URL is absent | A3 Security Scheme Object | specification / error | `security.openIdConnectUrl` | valid | `SecuritySchemeValidatorTest` |
| `AAS3-SECURITY-OPENID-URL-FORMAT` | OpenID Connect URL is not absolute | A3 Security Scheme Object | specification / error | `security.openIdConnectUrl` | valid | `SecuritySchemeValidatorTest` |

## Bindings and references

| Code | Object and condition | Authority | Concern / severity | Source field | Status | Test |
|---|---|---|---|---|---|---|
| `ADV-BINDING-EMPTY` | Bindings object is empty | Project guidance | advisory / warning | bindings object | advisory | `BindingValidatorTest` |
| `ADV-BINDING-PROTOCOL-NULL` | Binding protocol value is null | Project guidance | advisory / warning | protocol key | advisory | `BindingValidatorTest` |
| `AAS3-BINDING-PROTOCOL-TYPE` | Binding protocol value is not an object | A3 Binding Objects | specification / error | protocol key | incomplete: binding location/version context missing | `BindingValidatorTest` |
| `ADV-BINDING-PROPERTY-NULL` | Binding property is null | Project guidance | advisory / warning | binding property | advisory | `BindingValidatorTest` |
| `GEN-BINDING-PROPERTY-LIST` | Binding property list is rejected generically | Generator behavior | capability / warning | binding property | incorrect for fields such as Kafka cleanup policy | `BindingValidatorTest` |
| `ADV-BINDING-PROPERTY-EMPTY` | Binding property string is empty | Project guidance | advisory / warning | binding property | advisory | `BindingValidatorTest` |
| `GEN-BINDING-PROPERTY-TYPE` | Generic binding property type is unsupported | Generator behavior | capability / warning | binding property | incomplete: protocol/location-specific rules pending | `BindingValidatorTest` |
| `AAS3-REFERENCE-UNRESOLVED` | Reference cannot resolve | A3 Reference Object | specification / error | `$ref` | valid for reachable typed references | `OperationValidatorTest`, external-reference tests |
| `GEN-REFERENCE-CATEGORY-REQUIRED` | Parser-created reference lacks the concrete category required by its owning field | Generator/parser contract | capability / error | `$ref` | valid resilience boundary | `ReferenceIntegrityValidatorTest` |
| `AAS3-REFERENCE-TARGET-CATEGORY` | Reference resolves to an object outside the category required by its owning field | A3 Reference Object and owning field | specification / error | `$ref` | valid | `ReferenceIntegrityValidatorTest` |

## Schema Object

| Code | Object and condition | Authority | Concern / severity | Source field | Status | Test |
|---|---|---|---|---|---|---|
| `GEN-SCHEMA-KEYWORD-UNSUPPORTED` | Generator cannot process a schema keyword | Generator behavior | capability / error | keyword | generator-specific; policy audit pending | `SchemaValidatorTest` |
| `GEN-SCHEMA-ANNOTATION-IGNORED` | Generator ignores an annotation keyword | Generator behavior | capability / warning | keyword | generator-specific; policy audit pending | `SchemaValidatorTest` |
| `GEN-SCHEMA-ITEMS-REPRESENTATION` | Tuple-form or `false` boolean items cannot be represented by generated collection types | Generator behavior | capability / error | `items` | valid generator safeguard; tuple members remain parsed and validated | `SchemaValidatorTest` |
| `JSONSCHEMA-DEPENDENCY-ARRAY-ITEMS` | Property dependency contains a non-string member | JS7 dependencies | specification / error | dependency member | valid; parser also enforces this for source input | `SchemaValidatorTest` |
| `JSONSCHEMA-DEPENDENCY-ARRAY-NONEMPTY` | Property dependency array is empty | JS7 dependencies | specification / error | dependency array | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-DEPENDENCY-ARRAY-UNIQUE` | Property dependency repeats a property name | JS7 dependencies | specification / error | duplicate member | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-EXCLUSIVE-BOUND-TYPE` | Exclusive bound is not numeric | JS7 numeric validation | specification / error | `exclusiveMinimum` or `exclusiveMaximum` | valid | `SchemaValidatorTest` |
| `AAS3-SCHEMA-DISCRIMINATOR-TYPE` | Discriminator has an unsupported representation | A3 Schema | specification / error | `discriminator` | incomplete: AsyncAPI-specific behavior audit | `SchemaValidatorTest` |
| `GEN-SCHEMA-DIALECT` | Schema dialect is not the supported dialect | A3 Schema / generator behavior | capability / error | `schemaFormat` | incomplete: native and multi-format policy pending | `SchemaValidatorTest` |
| `JSONSCHEMA-TYPE` | Schema type value is not an exact supported string, or a type-array member is not a string | JS7 type | specification / error | `type` | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-TYPE-ARRAY-NONEMPTY` | Type array is empty | JS7 type | specification / error | `type` | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-TYPE-ARRAY-UNIQUE` | Type array contains duplicate values | JS7 type | specification / error | `type` | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-ENUM-NONEMPTY` | Enum is empty | JS7 enum | specification / error | `enum` | valid | `SchemaValidatorTest` |
| `JSONSCHEMA-ENUM-UNIQUE` | Enum contains JSON-equal duplicate values | JS7 enum | specification / error | `enum` | valid; numeric equality is precision-preserving | `SchemaValidatorTest` |
| `GEN-SCHEMA-UNTYPED-ENUM` | Untyped enum contains values the generator cannot safely infer as strings | Generator behavior | capability / error | `enum` | valid; all-string enum inference is generator-local | `SchemaValidatorTest` |
| `GEN-SCHEMA-CONST-TYPE` | Const cannot be represented by the generator's declared schema type | Generator behavior | capability / error | `const` | exact values and explicit null supported; absent type is not inferred | `SchemaValidatorTest` |
| `GEN-SCHEMA-NUMERIC-RANGE` | Lower numeric bound exceeds its corresponding upper bound | Generator constraint mapping | capability / error | lower bound | valid generator safeguard; contradictory JSON Schema is otherwise legal | `SchemaValidatorTest`, `BindingValidatorTest` |
| `JSONSCHEMA-MULTIPLE-OF` | `multipleOf` is not positive | JS7 numeric validation | specification / error | `multipleOf` | valid; arbitrary precision retained | `SchemaValidatorTest` |
| `JSONSCHEMA-STRING-LENGTH` | String length bound is not a nonnegative integer | JS7 string validation | specification / error | `minLength`, `maxLength` | valid | `SchemaValidatorTest` |
| `GEN-SCHEMA-STRING-LENGTH-RANGE` | Minimum string length exceeds maximum length | Generator constraint mapping | capability / error | `minLength` | valid generator safeguard | `SchemaValidatorTest` |
| `JSONSCHEMA-PATTERN` | Pattern is not accepted | JS7 string validation | specification / error | `pattern` | incomplete: ECMA-262 compatibility pending | `SchemaValidatorTest` |
| `JSONSCHEMA-ARRAY-SIZE` | Array size bound is not a nonnegative integer | JS7 array validation | specification / error | `minItems`, `maxItems` | valid | `SchemaValidatorTest` |
| `GEN-SCHEMA-ARRAY-SIZE-RANGE` | Minimum array size exceeds maximum size | Generator behavior | capability / error | `minItems` | valid generator safeguard | `SchemaValidatorTest` |
| `JSONSCHEMA-OBJECT-SIZE` | Object size bound is not a nonnegative integer | JS7 object validation | specification / error | `minProperties`, `maxProperties` | valid | `SchemaValidatorTest` |
| `GEN-SCHEMA-OBJECT-SIZE-RANGE` | Minimum property count exceeds maximum count | Generator behavior | capability / error | `minProperties` | valid generator safeguard | `SchemaValidatorTest` |
| `ADV-SCHEMA-REQUIRED-EMPTY` | Required list is empty | Project guidance | advisory / warning | `required` | advisory | `SchemaValidatorTest` |
| `JSONSCHEMA-REQUIRED-UNIQUE` | Required list contains duplicates | JS7 required | specification / error | `required` | valid | `SchemaValidatorTest` |
| `GEN-SCHEMA-REQUIRED-UNDECLARED` | Required property is not locally declared | Generator behavior | capability / warning | `required` | incomplete: composition/dependency awareness pending | `SchemaValidatorTest` |
| `GEN-SCHEMA-DEFAULT-TYPE` | Default cannot be represented by the generator's declared schema type | Generator behavior | capability / error | `default` | exact values and explicit null supported; absent type is not inferred | `SchemaValidatorTest` |
| `AAS3-SCHEMA-DISCRIMINATOR-REQUIRED` | Discriminator property is not required | A3 Schema | specification / error | `discriminator`, `required` | incomplete: conformance audit pending | `SchemaValidatorTest` |
| `AAS3-SCHEMA-DISCRIMINATOR-PROPERTY` | Discriminator property is not declared | A3 Schema | specification / error | `discriminator`, `properties` | incomplete: composition awareness pending | `SchemaValidatorTest` |

## Known unrepresented rule areas

The inventory exposes important gaps rather than assigning codes to behavior that does not exist yet: message example conformance against header and payload schemas, ECMA-262-compatible pattern handling, and Kafka server/channel/operation/message binding rules with binding version and location. These are later implementation slices, not implicit behavior of the rules above.
