package dev.banking.asyncapi.generator.core.generator.jsonschema

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.generator.sourcemodel.NameCase
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidJsonSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Renders supported contract declarations as standalone Draft 07 JSON Schema artifacts.
 */
internal class JsonSchemaGenerator(
    private val objectMapper: ObjectMapper =
        JsonMapper
            .builder()
            .defaultPropertyInclusion(
                JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.NON_NULL,
                ),
            ).build(),
) {
    fun render(
        schemaDeclarations: SchemaDeclarationCatalog,
        packageName: String,
    ): GenerationResult {
        val schemas = schemaDeclarations.asyncApiSchemas
        val nativeJsonSchemas =
            schemaDeclarations.multiFormatSchemas.filterValues { schema -> schema.format.isJsonSchemaDraft07 }
        val booleanSchemas = schemaDeclarations.booleanSchemas
        rejectSharedNames(
            "an AsyncAPI Schema Object" to schemas.keys,
            "a native JSON Schema" to nativeJsonSchemas.keys,
            "a Boolean schema" to booleanSchemas.keys,
        )

        val artifacts =
            buildList {
                schemas.entries
                    .sortedBy(Map.Entry<String, Schema>::key)
                    .forEach { (schemaName, schema) ->
                        add(
                            schemaArtifact(
                                schemaName = schemaName,
                                schemaNode = renderAsyncApiSchema(schema),
                                packageName = packageName,
                            ),
                        )
                    }
                nativeJsonSchemas.entries
                    .sortedBy(Map.Entry<String, MultiFormatSchema>::key)
                    .forEach { (schemaName, schema) ->
                        add(
                            schemaArtifact(
                                schemaName = schemaName,
                                schemaNode = renderNativeJsonSchema(schemaName, schema),
                                packageName = packageName,
                            ),
                        )
                    }
                booleanSchemas.entries
                    .sortedBy(Map.Entry<String, Boolean>::key)
                    .forEach { (schemaName, schema) ->
                        add(
                            schemaArtifact(
                                schemaName = schemaName,
                                schemaNode = objectMapper.nodeFactory.booleanNode(schema),
                                packageName = packageName,
                            ),
                        )
                    }
            }

        return GenerationResult(artifacts)
    }

    private fun renderAsyncApiSchema(schema: Schema): JsonNode {
        val schemaNode = objectMapper.valueToTree<ObjectNode>(schema)
        normalizeAsyncApiSchemaNode(schemaNode, schema)
        addDraft07Declaration(schemaNode)
        rewriteSchemaReferences(schemaNode)
        return schemaNode
    }

    private fun renderNativeJsonSchema(
        schemaName: String,
        schema: MultiFormatSchema,
    ): JsonNode {
        val schemaNode = objectMapper.valueToTree<JsonNode>(schema.schema)
        if (!schemaNode.isObject && !schemaNode.isBoolean) {
            throw InvalidJsonSchema(
                payloadName = schemaName,
                reason = "Draft 07 schema content must be an object or a boolean schema.",
            )
        }

        if (schemaNode is ObjectNode) {
            addDraft07Declaration(schemaNode)
        }
        rewriteSchemaReferences(schemaNode)
        return schemaNode
    }

    private fun schemaArtifact(
        schemaName: String,
        schemaNode: JsonNode,
        packageName: String,
    ): GeneratedArtifact =
        GeneratedArtifact(
            relativePath =
                GeneratedArtifactPaths.fromNamespace(
                    namespace = packageName,
                    fileName = "$schemaName.schema.json",
                ),
            content =
                objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(schemaNode)
                    .trimEnd() + System.lineSeparator(),
            kind = GeneratedArtifactKind.SCHEMA,
        )

    private fun normalizeAsyncApiSchemaNode(
        schemaNode: ObjectNode,
        schema: Schema,
    ) {
        schemaNode.remove(ASYNCAPI_ONLY_SCHEMA_FIELDS)
        if (schema.defaultSet && schema.default == null) {
            schemaNode.putNull("default")
        }
        if (schema.constSet && schema.const == null) {
            schemaNode.putNull("const")
        }

        SUBSCHEMA_KEYWORDS.forEach { keyword ->
            val node = schemaNode.get(keyword.name)
            when (keyword.shape) {
                Shape.SINGLE -> normalizeSchemaInterface(node, keyword.value(schema) as? SchemaInterface)
                Shape.MAP ->
                    (keyword.value(schema) as? Map<*, *>)?.forEach { (name, value) ->
                        normalizeSchemaInterface(node?.get(name.toString()), value as? SchemaInterface)
                    }
                Shape.LIST ->
                    (keyword.value(schema) as? List<*>)?.forEachIndexed { index, value ->
                        normalizeSchemaInterface(node?.get(index), value as? SchemaInterface)
                    }
            }
        }
        schema.tupleItems?.let { tupleItems -> schemaNode.set<JsonNode>("items", tupleArray(tupleItems)) }
    }

    /** Draft 7 tuple-form `items`: inline members normalized, references and Boolean schemas as written. */
    private fun tupleArray(tupleItems: List<SchemaInterface>): ArrayNode {
        val tupleArray = objectMapper.createArrayNode()
        tupleItems.forEach { element ->
            when (element) {
                is SchemaInterface.SchemaInline -> {
                    val elementNode = objectMapper.valueToTree<ObjectNode>(element.schema)
                    normalizeAsyncApiSchemaNode(elementNode, element.schema)
                    tupleArray.add(elementNode)
                }
                is SchemaInterface.SchemaReference -> {
                    val refNode = objectMapper.createObjectNode()
                    refNode.put($$"$ref", element.reference.ref)
                    tupleArray.add(refNode)
                }
                is SchemaInterface.BooleanSchema -> {
                    tupleArray.add(element.value)
                }
                else -> {
                    tupleArray.add(objectMapper.createObjectNode())
                }
            }
        }
        return tupleArray
    }

    private fun normalizeSchemaInterface(
        node: JsonNode?,
        schema: SchemaInterface?,
    ) {
        if (schema is SchemaInterface.SchemaInline && node is ObjectNode) {
            normalizeAsyncApiSchemaNode(node, schema.schema)
        }
    }

    private fun addDraft07Declaration(schemaNode: ObjectNode) {
        if (!schemaNode.has($$"$schema")) {
            schemaNode.put($$"$schema", DRAFT_07_SCHEMA_URI)
        }
    }

    private fun rewriteSchemaReferences(schemaNode: JsonNode) {
        if (schemaNode !is ObjectNode) {
            return
        }

        schemaNode
            .get($$"$ref")
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
            ?.let(::generatedSchemaReference)
            ?.let { reference -> schemaNode.put($$"$ref", reference) }

        SUBSCHEMA_KEYWORDS.forEach { keyword ->
            val node = schemaNode.get(keyword.name)
            if (keyword.shape == Shape.MAP) {
                (node as? ObjectNode)?.elements()?.forEachRemaining(::rewriteSchemaNodeReference)
            } else {
                rewriteSchemaNodeReference(node)
            }
        }
    }

    /** Rewrites the references of a schema, or of every schema in an array of them. */
    private fun rewriteSchemaNodeReference(node: JsonNode?) {
        when (node) {
            is ObjectNode -> rewriteSchemaReferences(node)
            is ArrayNode -> node.elements().forEachRemaining(::rewriteSchemaNodeReference)
            else -> Unit
        }
    }

    /** Fails on the first generated artifact name that two kinds of schema both use. */
    private fun rejectSharedNames(vararg kinds: Pair<String, Set<String>>) {
        kinds.forEachIndexed { index, (firstKind, firstNames) ->
            kinds.drop(index + 1).forEach { (secondKind, secondNames) ->
                firstNames.intersect(secondNames).minOrNull()?.let { name ->
                    throw InvalidJsonSchema(
                        payloadName = name,
                        reason = "Both $firstKind and $secondKind use this generated artifact name.",
                    )
                }
            }
        }
    }

    private fun generatedSchemaReference(reference: String): String {
        if (!reference.startsWith(COMPONENT_SCHEMA_REFERENCE_PREFIX)) {
            return reference
        }

        val target = reference.removePrefix(COMPONENT_SCHEMA_REFERENCE_PREFIX)
        val componentName = target.substringBefore('/')
        val componentPath = target.substringAfter('/', missingDelimiterValue = "")
        val generatedName = NameCase.toPascalCase(componentName)
        val fragment = componentPath.takeIf(String::isNotEmpty)?.let { "#/$it" }.orEmpty()
        return "$generatedName.schema.json$fragment"
    }

    /** How a keyword holds sub-schemas: one schema, a map of named schemas, or a list of schemas. */
    private enum class Shape { SINGLE, MAP, LIST }

    /**
     * A keyword whose value holds sub-schemas.
     *
     * @property name the keyword
     * @property shape how the keyword holds its sub-schemas
     * @property value the keyword's value in a parsed schema
     */
    private class SubschemaKeyword(
        val name: String,
        val shape: Shape,
        val value: (Schema) -> Any?,
    )

    private companion object {
        /**
         * The Draft 7 keywords that hold sub-schemas, which both passes over a schema visit. Tuple-form
         * `items` is an array of schemas, so a single `items` keyword visits it too.
         */
        val SUBSCHEMA_KEYWORDS =
            listOf(
                SubschemaKeyword("items", Shape.SINGLE) { it.items },
                SubschemaKeyword("additionalItems", Shape.SINGLE) { it.additionalItems },
                SubschemaKeyword("contains", Shape.SINGLE) { it.contains },
                SubschemaKeyword("properties", Shape.MAP) { it.properties },
                SubschemaKeyword("patternProperties", Shape.MAP) { it.patternProperties },
                SubschemaKeyword("additionalProperties", Shape.SINGLE) { it.additionalProperties },
                SubschemaKeyword("propertyNames", Shape.SINGLE) { it.propertyNames },
                SubschemaKeyword("dependencies", Shape.MAP) { it.dependencies },
                SubschemaKeyword("definitions", Shape.MAP) { it.definitions },
                SubschemaKeyword("allOf", Shape.LIST) { it.allOf },
                SubschemaKeyword("anyOf", Shape.LIST) { it.anyOf },
                SubschemaKeyword("oneOf", Shape.LIST) { it.oneOf },
                SubschemaKeyword("not", Shape.SINGLE) { it.not },
                SubschemaKeyword("if", Shape.SINGLE) { it.ifSchema },
                SubschemaKeyword("then", Shape.SINGLE) { it.thenSchema },
                SubschemaKeyword("else", Shape.SINGLE) { it.elseSchema },
            )

        const val DRAFT_07_SCHEMA_URI = "http://json-schema.org/draft-07/schema#"
        const val COMPONENT_SCHEMA_REFERENCE_PREFIX = "#/components/schemas/"

        val ASYNCAPI_ONLY_SCHEMA_FIELDS =
            listOf(
                "discriminator",
                "externalDocs",
                "deprecated",
                "bindings",
            )
    }
}
