package com.tietoevry.banking.asyncapi.generator.core.generator.analyzer

import com.tietoevry.banking.asyncapi.generator.core.model.schemas.Schema
import com.tietoevry.banking.asyncapi.generator.core.model.schemas.SchemaInterface

class PolymorphicAnalyzer : AnalysisStage<Map<String, List<String>>> {

    override fun analyze(schemas: Map<String, Schema>): Map<String, List<String>> {
        val childToParents = mutableMapOf<String, MutableList<String>>()

        // Iterate through all schemas to find the ones that act as parents (i.e., have `oneOf` or `anyOf`)
        schemas.forEach { (parentInterfaceName, schema) ->
            val childSchemaRefs = schema.oneOf ?: schema.anyOf ?: emptyList()

            childSchemaRefs.forEach { childSchemaInterface ->
                if (childSchemaInterface is SchemaInterface.SchemaReference) {
                    val childName = childSchemaInterface.reference.ref.substringAfterLast('/')

                    // For each child, add an entry mapping it back to its parent.
                    childToParents.getOrPut(childName) { mutableListOf() }.add(parentInterfaceName)
                }
            }
        }
        return childToParents
    }
}
