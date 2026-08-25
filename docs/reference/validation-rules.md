# Validation rule inventory

This reference records implemented validation rules and their ownership.

| Code | Condition | Authority | Concern / severity |
|---|---|---|---|
| `AAS3-DOCUMENT-ID-FORMAT` | Document `id` is not a URI. | AsyncAPI 3.0 | specification / error |
| `ADV-DOCUMENT-ID-URN` | Document `id` is not a URN. | Project guidance | advisory / warning |
| `AAS3-DOCUMENT-CONTENT-TYPE` | `defaultContentType` is not a specific media type. | AsyncAPI 3.0 | specification / error |
| `AAS3-INFO-TITLE-REQUIRED` | `info.title` is empty. | AsyncAPI 3.0 Info Object | specification / error |
| `AAS3-INFO-VERSION-REQUIRED` | `info.version` is empty. | AsyncAPI 3.0 Info Object | specification / error |
| `AAS3-INFO-TERMS-URI` | `info.termsOfService` is not an absolute URI. | AsyncAPI 3.0 Info Object | specification / error |
| `AAS3-CONTACT-URL-FORMAT` | Contact URL is not an absolute URI. | AsyncAPI 3.0 Contact Object | specification / error |
| `AAS3-CONTACT-EMAIL-FORMAT` | Contact email is malformed. | AsyncAPI 3.0 Contact Object | specification / error |
| `AAS3-LICENSE-NAME-REQUIRED` | License name is empty. | AsyncAPI 3.0 License Object | specification / error |
| `AAS3-LICENSE-URL-FORMAT` | License URL is not an absolute URI. | AsyncAPI 3.0 License Object | specification / error |
| `AAS3-EXTERNAL-DOC-URL-REQUIRED` | External documentation URL is empty. | AsyncAPI 3.0 External Documentation Object | specification / error |
| `AAS3-EXTERNAL-DOC-URL-FORMAT` | External documentation URL is not an absolute URI. | AsyncAPI 3.0 External Documentation Object | specification / error |
| `AAS3-TAG-NAME-REQUIRED` | Tag name is empty. | AsyncAPI 3.0 Tag Object | specification / error |
| `AAS3-SERVER-HOST-REQUIRED` | Server host is empty. | AsyncAPI 3.0 Server Object | specification / error |
| `AAS3-SERVER-NAME-FORMAT` | Server map key uses unsupported characters. | AsyncAPI 3.0 Servers Object | specification / error |
| `ADV-SERVER-HOST-PROTOCOL` | Host contains a protocol scheme. | Project guidance | advisory / warning |
| `AAS3-SERVER-VARIABLE-UNDEFINED` | Host or pathname variable lacks a definition. | AsyncAPI 3.0 Server Object | specification / error |
| `AAS3-SERVER-PROTOCOL-REQUIRED` | Server protocol is empty. | AsyncAPI 3.0 Server Object | specification / error |
| `AAS3-SERVER-VARIABLE-ENUM-UNIQUE` | Server-variable enum repeats values. | AsyncAPI 3.0 Server Variable Object | specification / error |
| `ADV-SERVER-VARIABLE-DEFAULT-ENUM` | Default is outside enum. | Project guidance | advisory / warning |
| `ADV-SERVER-VARIABLE-EXAMPLES-ENUM` | Example is outside enum. | Project guidance | advisory / warning |
| `AAS3-CHANNEL-ADDRESS-SUFFIX` | Channel address contains a query or fragment suffix. | AsyncAPI 3.0 Channel Object | specification / error |
| `AAS3-CHANNEL-PARAMETER-UNDEFINED` | Channel address parameter is undefined. | AsyncAPI 3.0 Channel Object | specification / error |
| `AAS3-CHANNEL-PARAMETER-UNUSED` | Parameters are present without matching address expressions. | AsyncAPI 3.0 Channel Object | specification / error |
| `AAS3-MESSAGE-CONTENT-TYPE` | Message or message-trait content type is not a specific media type. | AsyncAPI 3.0 Message Object | specification / error |
| `AAS3-MESSAGE-EXAMPLE-CONTENT` | Message example contains neither headers nor payload. | AsyncAPI 3.0 Message Example Object | specification / error |
| `AAS3-MESSAGE-EXAMPLE-SCHEMA` | Message example does not satisfy its AsyncAPI Schema Object. | AsyncAPI 3.0 Message/Schema Object | specification / error |
| `GEN-MESSAGE-EXAMPLE-FORMAT` | Multi Format Schema example has no proven instance validator. | Generator behavior | capability / warning |
| `GEN-MESSAGE-HEADER-FORMAT` | Message header schema form is unsupported. | Generator behavior | capability / warning |
| `AAS3-PARAMETER-ENUM-UNIQUE` | Parameter enum repeats values. | AsyncAPI 3.0 Parameter Object | specification / error |
| `AAS3-PARAMETER-NAME-FORMAT` | Parameter map key uses unsupported characters. | AsyncAPI 3.0 Parameters Object | specification / error |
| `ADV-PARAMETER-DEFAULT-ENUM` | Parameter default is outside enum. | Project guidance | advisory / warning |
| `ADV-PARAMETER-EXAMPLES-ENUM` | Parameter example is outside enum. | Project guidance | advisory / warning |
| `AAS3-PARAMETER-LOCATION` | Parameter location expression is malformed. | AsyncAPI 3.0 Parameter Object | specification / error |
| `AAS3-CORRELATION-LOCATION-REQUIRED` | Correlation location is empty. | AsyncAPI 3.0 Correlation ID Object | specification / error |
| `AAS3-CORRELATION-LOCATION-FORMAT` | Correlation location expression is malformed. | AsyncAPI 3.0 Runtime Expressions | specification / error |
| `AAS3-OPERATION-ACTION-REQUIRED` | Operation `action` is absent. | AsyncAPI 3.0 Operation Object | specification / error |
| `AAS3-OPERATION-ACTION-VALUE` | Operation action is not `send` or `receive`. | AsyncAPI 3.0 Operation Object | specification / error |
| `AAS3-OPERATION-CHANNEL-TARGET` | Operation channel reference resolves to another category. | AsyncAPI 3.0 Operation Object | specification / error |
| `AAS3-OPERATION-CHANNEL-SCOPE` | Root operation channel is not owned by root `channels`. | AsyncAPI 3.0 Operation Object | specification / error |
| `AAS3-OPERATION-MESSAGE-REFERENCE` | Operation message is not a valid channel message. | AsyncAPI 3.0 Operation Object | specification / error |
| `AAS3-REPLY-CHANNEL-REQUIRED` | Reply defines messages without a channel. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-REPLY-CHANNEL-SCOPE` | Inline root-operation reply channel is not owned by root `channels`. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-REPLY-CHANNEL-ADDRESS` | Reply defines an address while its channel has a known address. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-REPLY-MESSAGE-REFERENCE` | Reply message does not reference a message owned by reply channel. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-REPLY-ADDRESS-REQUIRED` | Reply defines a message set without `address`. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-REPLY-ADDRESS-FORMAT` | Reply address expression is malformed. | AsyncAPI 3.0 Operation Reply Object | specification / error |
| `AAS3-SECURITY-TYPE-REQUIRED` | Security type is missing. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-TYPE-VALUE` | Security type value is unknown. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-NAME-REQUIRED` | Security name is empty. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-IN-REQUIRED` | Security `in` field is missing for `apiKey`/`httpApiKey`. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-IN-VALUE` | Security `in` is not one of `query`, `header`, or `cookie`. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-SCHEME-REQUIRED` | Security scheme is missing (`apiKey`, `openIdConnect`, OAuth scopes, etc.). | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-OAUTH-FLOWS` | OAuth2 security has no `flows`. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-OAUTH-AUTHORIZATION-URL-REQUIRED` | OAuth flow with `implicit` is missing `authorizationUrl`. | AsyncAPI 3.0 OAuth Flow Object | specification / error |
| `AAS3-OAUTH-AUTHORIZATION-URL-FORMAT` | OAuth flow `authorizationUrl` is not absolute. | AsyncAPI 3.0 OAuth Flow Object | specification / error |
| `AAS3-OAUTH-TOKEN-URL-REQUIRED` | OAuth flow with `password`/`clientCredentials`/`authorizationCode` missing `tokenUrl`. | AsyncAPI 3.0 OAuth Flow Object | specification / error |
| `AAS3-OAUTH-TOKEN-URL-FORMAT` | OAuth flow `tokenUrl` is not absolute. | AsyncAPI 3.0 OAuth Flow Object | specification / error |
| `AAS3-OAUTH-REFRESH-URL-FORMAT` | OAuth flow `refreshUrl` is not absolute. | AsyncAPI 3.0 OAuth Flow Object | specification / error |
| `AAS3-OAUTH-SCOPE-AVAILABLE` | OAuth scopes reference unavailable scope at scheme level. | AsyncAPI 3.0 Security Scheme and OAuth Flow Objects | specification / error |
| `AAS3-SECURITY-OPENID-URL-REQUIRED` | OpenID Connect URL is absent. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-SECURITY-OPENID-URL-FORMAT` | OpenID Connect URL is not absolute. | AsyncAPI 3.0 Security Scheme Object | specification / error |
| `AAS3-BINDING-PROTOCOL-TYPE` | Binding protocol value is not an object with string property names. | AsyncAPI 3.0 Binding Objects | specification / error |
| `AAS3-KAFKA-BINDING-VERSION-TYPE` | Kafka `bindingVersion` is not a string. | Kafka binding | specification / error |
| `GEN-KAFKA-BINDING-VERSION` | Kafka binding version is outside supported versions. | Kafka binding / generator behavior | capability / error |
| `AAS3-KAFKA-BINDING-FIELD` | Kafka binding contains a field not defined at the current location. | Kafka binding | specification / error |
| `AAS3-KAFKA-BINDING-FIELD-TYPE` | Kafka binding field has the wrong type. | Kafka binding | specification / error |
| `AAS3-KAFKA-CHANNEL-POSITIVE-INTEGER` | Kafka channel binding numeric fields are not positive integers. | Kafka binding | specification / error |
| `AAS3-KAFKA-SCHEMA-REGISTRY-URL` | Kafka schema-registry URL is not absolute. | Kafka binding | specification / error |
| `AAS3-KAFKA-SCHEMA-REGISTRY-VENDOR-URL` | Kafka schema-registry vendor is present without schema registry URL. | Kafka binding | specification / error |
| `AAS3-KAFKA-MESSAGE-SCHEMA-REGISTRY-URL` | Message schema-ID field requires schema-registry URL on applicable servers. | Kafka binding | specification / error |
| `AAS3-KAFKA-CLEANUP-POLICY` | Kafka cleanup policy is invalid. | Kafka channel binding | specification / error |
| `AAS3-REFERENCE-UNRESOLVED` | Reference cannot resolve. | AsyncAPI reference objects | specification / error |
| `GEN-REFERENCE-CATEGORY-REQUIRED` | Parsed reference is missing required concrete category. | Parser and generator contract | capability / error |
| `AAS3-REFERENCE-TARGET-CATEGORY` | Reference resolves to mismatched target category. | AsyncAPI reference objects | specification / error |
| `GEN-SCHEMA-KEYWORD-UNSUPPORTED` | Unsupported Schema keyword for this generator. | Generator behavior | capability / error |
| `GEN-SCHEMA-ANNOTATION-IGNORED` | Known Schema annotation keyword is ignored by generated runtimes. | Generator behavior | capability / warning |
| `GEN-SCHEMA-ITEMS-REPRESENTATION` | Retained legacy identifier; no longer emitted. | Generator behavior | capability / error |
| `JSONSCHEMA-DEPENDENCY-ARRAY-UNIQUE` | Property dependency array repeats a property name. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-DIALECT` | Schema `$schema` is not the supported Draft 7 dialect. | JSON Schema Draft 7 / generator behavior | capability / error |
| `JSONSCHEMA-TYPE` | Schema `type` is not an exact supported string or array member. | JSON Schema Draft 7 | specification / error |
| `JSONSCHEMA-TYPE-ARRAY-NONEMPTY` | Type array is empty. | JSON Schema Draft 7 | specification / error |
| `JSONSCHEMA-TYPE-ARRAY-UNIQUE` | Type array contains duplicates. | JSON Schema Draft 7 | specification / error |
| `JSONSCHEMA-ENUM-NONEMPTY` | Enum is empty. | JSON Schema Draft 7 | specification / error |
| `JSONSCHEMA-ENUM-UNIQUE` | Enum repeats JSON-equal duplicate values. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-UNTYPED-ENUM` | Retained legacy identifier; no longer emitted. | Generator behavior | capability / error |
| `GEN-SCHEMA-CONST-TYPE` | `const` cannot be represented by generator model type. | Generator behavior | capability / error |
| `GEN-SCHEMA-NUMERIC-RANGE` | Numeric lower bound is greater than upper bound or invalid. | Generator constraint mapping | capability / error |
| `JSONSCHEMA-MULTIPLE-OF` | `multipleOf` is not positive. | JSON Schema Draft 7 | specification / error |
| `JSONSCHEMA-STRING-LENGTH` | String length bound is not a nonnegative integer. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-STRING-LENGTH-RANGE` | Minimum string length exceeds maximum length. | Generator constraint mapping | capability / error |
| `GEN-SCHEMA-PATTERN` | Retained legacy identifier; no longer emitted. | Generator behavior | capability / error |
| `JSONSCHEMA-ARRAY-SIZE` | Array size bound is not a nonnegative integer. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-ARRAY-SIZE-RANGE` | Minimum array size exceeds maximum size. | Generator behavior | capability / error |
| `JSONSCHEMA-OBJECT-SIZE` | Object size bound is not a nonnegative integer. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-OBJECT-SIZE-RANGE` | Minimum object size exceeds maximum size. | Generator behavior | capability / error |
| `JSONSCHEMA-REQUIRED-UNIQUE` | `required` list contains duplicates. | JSON Schema Draft 7 | specification / error |
| `GEN-SCHEMA-REQUIRED-UNDECLARED` | Required property is missing from declaration scope. | Generator behavior | capability / warning |
| `GEN-SCHEMA-DEFAULT-TYPE` | `default` cannot be represented by declared generator type. | Generator behavior | capability / error |
| `AAS3-SCHEMA-DISCRIMINATOR-REQUIRED` | Discriminator property is not locally required. | AsyncAPI Schema Object | specification / error |
| `AAS3-SCHEMA-DISCRIMINATOR-PROPERTY` | Discriminator property is not locally declared. | AsyncAPI Schema Object | specification / error |

`GEN-SCHEMA-ITEMS-REPRESENTATION`, `GEN-SCHEMA-UNTYPED-ENUM`, and `GEN-SCHEMA-PATTERN` remain public legacy identifiers but are no longer emitted. Output-specific incompatibilities for these schema features are reported by generation compatibility exceptions.
