package dev.banking.asyncapi.generator.core.validator.schemas

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.util.Collections
import java.util.IdentityHashMap

/** Finds statically declared properties that compose one object through `allOf`. */
internal class SchemaPropertyDeclarations(
    private val asyncApiContext: AsyncApiContext,
) {
    fun collect(schema: Schema): Set<String> =
        buildSet { collectSchema(schema, this, newVisitedSet()) }

    private fun collectSchema(
        schema: Schema,
        declarations: MutableSet<String>,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(schema)) return
        declarations += schema.properties.orEmpty().keys
        schema.allOf.orEmpty().forEach { collectInterface(it, declarations, visited) }
    }

    private fun collectInterface(
        schema: SchemaInterface,
        declarations: MutableSet<String>,
        visited: MutableSet<Any>,
    ) {
        when (schema) {
            is SchemaInterface.SchemaInline -> collectSchema(schema.schema, declarations, visited)
            is SchemaInterface.SchemaReference -> collectReference(schema.reference, declarations, visited)
            is SchemaInterface.BooleanSchema,
            is SchemaInterface.MultiFormatSchemaInline,
            -> Unit
        }
    }

    private fun collectReference(
        reference: Reference,
        declarations: MutableSet<String>,
        visited: MutableSet<Any>,
    ) {
        if (!visited.add(reference)) return
        when (val target = asyncApiContext.findReference(reference)) {
            is Schema -> collectSchema(target, declarations, visited)
            is Reference -> collectReference(target, declarations, visited)
            is SchemaInterface -> collectInterface(target, declarations, visited)
        }
    }

    private fun newVisitedSet(): MutableSet<Any> =
        Collections.newSetFromMap(IdentityHashMap())
}
