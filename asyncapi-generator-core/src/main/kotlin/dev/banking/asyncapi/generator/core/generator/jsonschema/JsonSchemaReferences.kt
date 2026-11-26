package dev.banking.asyncapi.generator.core.generator.jsonschema

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import dev.banking.asyncapi.generator.core.generator.sourcemodel.NameCase

/**
 * Rewrites references to component schemas into references to the generated schema files, such as
 * `#/components/schemas/account` into `Account.schema.json`, in a schema and all its sub-schemas.
 */
internal object JsonSchemaReferences {
    private const val COMPONENT_SCHEMA_REFERENCE_PREFIX = "#/components/schemas/"

    /** Rewrites the component references of [schemaNode] and its sub-schemas in place. */
    fun rewrite(schemaNode: JsonNode) {
        if (schemaNode !is ObjectNode) {
            return
        }

        schemaNode
            .get($$"$ref")
            ?.takeIf(JsonNode::isTextual)
            ?.asText()
            ?.let(::generatedSchemaReference)
            ?.let { reference -> schemaNode.put($$"$ref", reference) }

        SubschemaKeyword.ALL.forEach { keyword ->
            val node = schemaNode.get(keyword.name)
            if (keyword.shape == SubschemaKeyword.Shape.MAP) {
                (node as? ObjectNode)?.elements()?.forEachRemaining(::rewriteSchemaNodeReference)
            } else {
                rewriteSchemaNodeReference(node)
            }
        }
    }

    /** Rewrites the references of a schema, or of every schema in an array of them. */
    private fun rewriteSchemaNodeReference(node: JsonNode?) {
        when (node) {
            is ObjectNode -> rewrite(node)
            is ArrayNode -> node.elements().forEachRemaining(::rewriteSchemaNodeReference)
            else -> Unit
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
}
