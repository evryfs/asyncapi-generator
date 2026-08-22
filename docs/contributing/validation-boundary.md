# Validation boundary

This guide explains where a new validation check belongs: in the semantic validator or in the generation compatibility validator.

## The two validators

### AsyncApiValidator

Checks semantic conformance, reference integrity, advisories, and generator-wide supported-contract constraints.

This validator runs on every parsed contract, regardless of what the generator will produce. It answers: "Is this a valid AsyncAPI document that the generator can accept?"

### GenerationInputCompatibilityValidator

Checks whether a specific requested output can consume the prepared contract.

This validator runs after the generation plan is created. It answers: "Can the selected generator capabilities produce the requested artifacts from this contract?"

## Where to put a new check

Use this rule:

> Generator-wide supported-contract constraints apply independently of the selected output target. Restrictions that exist only because a particular model, schema, language, or client backend was selected belong to generation compatibility validation.

### Examples

| Check                                                      | Validator                               | Reason                                                |
|------------------------------------------------------------|-----------------------------------------|-------------------------------------------------------|
| Channel has valid address format                           | `AsyncApiValidator`                     | Applies to all contracts regardless of output         |
| Schema has valid JSON Schema structure                     | `AsyncApiValidator`                     | Applies to all contracts regardless of output         |
| Native Protobuf input is available for Protobuf generation | `GenerationInputCompatibilityValidator` | Only applies when Protobuf generation is selected     |
| Spring Kafka contract has valid method names               | `GenerationInputCompatibilityValidator` | Only applies when Spring Kafka generation is selected |
| Avro schema has valid structure for Avro projection        | `GenerationInputCompatibilityValidator` | Only applies when Avro projection is selected         |

### Decision flowchart

1. Does the check apply to every contract the generator accepts, regardless of output target?
   - **Yes** → `AsyncApiValidator`
   - **No** → Continue to 2
2. Does the check only matter because a specific generator capability was selected?
   - **Yes** → `GenerationInputCompatibilityValidator`
   - **No** → Continue to 3
3. Does the check validate the contract against the AsyncAPI specification?
   - **Yes** → `AsyncApiValidator`
   - **No** → Consider whether the check belongs in the pipeline at all

## Why this matters

- **Semantic validation** catches problems early, before generation planning. Users see these errors even if they haven't configured a generator.
- **Compatibility validation** catches problems late, after generation planning. Users see these errors only when their configuration requires capabilities the contract doesn't support.

Mixing these concerns makes it harder for contributors to understand where checks belong and harder for users to understand why a check failed.
