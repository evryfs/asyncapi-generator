package dev.banking.asyncapi.generator.core.generator.jsonschema

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.json.JsonMapper
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.schema.SchemaDeclarationCatalog
import dev.banking.asyncapi.generator.core.generator.util.MapperUtil
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidJsonSchema
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Renders supported contract declarations as standalone Draft 07 JSON Schema artifacts.
 */
class JsonSchemaGenerator(
    private val objectMapper: ObjectMapper =
        JsonMapper.builder()
            .defaultPropertyInclusion(
                JsonInclude.Value.construct(
                    JsonInclude.Include.NON_NULL,
                    JsonInclude.Include.NON_NULL,
                ),
            )
            .build(),
) {
    fun render(
        schemaDeclarations: SchemaDeclarationCatalog,
        packageName: String,
    ): GenerationResult {
        val schemas = schemaDeclarations.asyncApiSchemas
        val nativeJsonSchemas =
            schemaDeclarations.multiFormatSchemas.filterValues { schema -> schema.format.isJsonSchemaDraft07 }
        val booleanSchemas = schemaDeclarations.booleanSchemas
        val asyncApiNativeDuplicate = schemas.keys.intersect(nativeJsonSchemas.keys).minOrNull()
        if (asyncApiNativeDuplicate != null) {
            throw InvalidJsonSchema(
                payloadName = asyncApiNativeDuplicate,
                reason = "Both an AsyncAPI Schema Object and a native JSON Schema use this generated artifact name.",
            )
        }
        val asyncApiBooleanDuplicate = schemas.keys.intersect(booleanSchemas.keys).minOrNull()
        if (asyncApiBooleanDuplicate != null) {
            throw InvalidJsonSchema(
                payloadName = asyncApiBooleanDuplicate,
                reason = "Both an AsyncAPI Schema Object and a Boolean schema use this generated artifact name.",
            )
        }
        val nativeBooleanDuplicate = nativeJsonSchemas.keys.intersect(booleanSchemas.keys).minOrNull()
        if (nativeBooleanDuplicate != null) {
            throw InvalidJsonSchema(
                payloadName = nativeBooleanDuplicate,
                reason = "Both a native JSON Schema and a Boolean schema use this generated artifact name.",
            )
        }

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

        normalizeSchemaInterface(schemaNode.get("items"), schema.items)
        schema.tupleItems?.let { tupleItems ->
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
            schemaNode.set<JsonNode>("items", tupleArray)
        }
        normalizeSchemaInterface(schemaNode.get("additionalItems"), schema.additionalItems)
        normalizeSchemaInterface(schemaNode.get("contains"), schema.contains)
        normalizeSchemaMap(schemaNode.get("properties"), schema.properties)
        normalizeSchemaMap(schemaNode.get("patternProperties"), schema.patternProperties)
        normalizeSchemaInterface(schemaNode.get("additionalProperties"), schema.additionalProperties)
        normalizeSchemaInterface(schemaNode.get("propertyNames"), schema.propertyNames)
        normalizeDependencies(schemaNode.get("dependencies"), schema.dependencies)
        normalizeSchemaMap(schemaNode.get("definitions"), schema.definitions)
        normalizeSchemaList(schemaNode.get("allOf"), schema.allOf)
        normalizeSchemaList(schemaNode.get("anyOf"), schema.anyOf)
        normalizeSchemaList(schemaNode.get("oneOf"), schema.oneOf)
        normalizeSchemaInterface(schemaNode.get("not"), schema.not)
        normalizeSchemaInterface(schemaNode.get("if"), schema.ifSchema)
        normalizeSchemaInterface(schemaNode.get("then"), schema.thenSchema)
        normalizeSchemaInterface(schemaNode.get("else"), schema.elseSchema)
    }

    private fun normalizeSchemaMap(
        node: JsonNode?,
        schemas: Map<String, SchemaInterface>?,
    ) {
        val objectNode = node as? ObjectNode ?: return
        schemas?.forEach { (name, schema) ->
            normalizeSchemaInterface(objectNode.get(name), schema)
        }
    }

    private fun normalizeSchemaList(
        node: JsonNode?,
        schemas: List<SchemaInterface>?,
    ) {
        val arrayNode = node as? ArrayNode ?: return
        schemas?.forEachIndexed { index, schema ->
            normalizeSchemaInterface(arrayNode.get(index), schema)
        }
    }

    private fun normalizeDependencies(
        node: JsonNode?,
        dependencies: Map<String, Any>?,
    ) {
        val objectNode = node as? ObjectNode ?: return
        dependencies?.forEach { (name, dependency) ->
            if (dependency is SchemaInterface) {
                normalizeSchemaInterface(objectNode.get(name), dependency)
            }
        }
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

        schemaNode.get($$"$ref")
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
            ?.let(::generatedSchemaReference)
            ?.let { reference -> schemaNode.put($$"$ref", reference) }

        rewriteSchemaMapReferences(schemaNode.get("properties"))
        rewriteSchemaMapReferences(schemaNode.get("patternProperties"))
        rewriteSchemaMapReferences(schemaNode.get("definitions"))
        rewriteDependencyReferences(schemaNode.get("dependencies"))
        rewriteSchemaNodeReference(schemaNode.get("additionalProperties"))
        rewriteSchemaNodeReference(schemaNode.get("additionalItems"))
        rewriteSchemaNodeReference(schemaNode.get("items"))
        rewriteSchemaNodeReference(schemaNode.get("contains"))
        rewriteSchemaNodeReference(schemaNode.get("propertyNames"))
        rewriteSchemaNodeReference(schemaNode.get("not"))
        rewriteSchemaNodeReference(schemaNode.get("if"))
        rewriteSchemaNodeReference(schemaNode.get("then"))
        rewriteSchemaNodeReference(schemaNode.get("else"))
        rewriteSchemaArrayReferences(schemaNode.get("allOf"))
        rewriteSchemaArrayReferences(schemaNode.get("anyOf"))
        rewriteSchemaArrayReferences(schemaNode.get("oneOf"))
    }

    private fun rewriteSchemaMapReferences(node: JsonNode?) {
        val objectNode = node as? ObjectNode ?: return
        objectNode.elements().forEachRemaining(::rewriteSchemaNodeReference)
    }

    private fun rewriteDependencyReferences(node: JsonNode?) {
        val objectNode = node as? ObjectNode ?: return
        objectNode.elements().forEachRemaining { dependency ->
            if (!dependency.isArray) {
                rewriteSchemaNodeReference(dependency)
            }
        }
    }

    private fun rewriteSchemaArrayReferences(node: JsonNode?) {
        val arrayNode = node as? ArrayNode ?: return
        arrayNode.elements().forEachRemaining(::rewriteSchemaNodeReference)
    }

    private fun rewriteSchemaNodeReference(node: JsonNode?) {
        when {
            node is ObjectNode -> rewriteSchemaReferences(node)
            node is ArrayNode -> node.elements().forEachRemaining(::rewriteSchemaNodeReference)
        }
    }

    private fun generatedSchemaReference(reference: String): String {
        if (!reference.startsWith(COMPONENT_SCHEMA_REFERENCE_PREFIX)) {
            return reference
        }

        val target = reference.removePrefix(COMPONENT_SCHEMA_REFERENCE_PREFIX)
        val componentName = target.substringBefore('/')
        val componentPath = target.substringAfter('/', missingDelimiterValue = "")
        val generatedName = MapperUtil.toPascalCase(componentName)
        val fragment = componentPath.takeIf(String::isNotEmpty)?.let { "#/$it" }.orEmpty()
        return "$generatedName.schema.json$fragment"
    }

    private companion object {
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
