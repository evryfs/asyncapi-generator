package dev.banking.asyncapi.generator.core.generator.analyzer

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Records which unions each schema is a member of: a schema with `oneOf`, or
 * `anyOf` when it has no `oneOf`, is the union of the schemas it references.
 */
internal class PolymorphicAnalyzer {
    /** The names of the unions each member schema belongs to, by member name. */
    fun analyze(schemas: Map<String, Schema>): Map<String, List<String>> {
        val childToParents = mutableMapOf<String, MutableList<String>>()
        schemas.forEach { (parentInterfaceName, schema) ->
            val childSchemaRefs = schema.oneOf ?: schema.anyOf ?: emptyList()

            childSchemaRefs.forEach { childSchemaInterface ->
                if (childSchemaInterface is SchemaInterface.SchemaReference) {
                    val childName = childSchemaInterface.reference.generatedSchemaName()
                    childToParents.getOrPut(childName) { mutableListOf() }.add(parentInterfaceName)
                }
            }
        }
        return childToParents
    }
}
