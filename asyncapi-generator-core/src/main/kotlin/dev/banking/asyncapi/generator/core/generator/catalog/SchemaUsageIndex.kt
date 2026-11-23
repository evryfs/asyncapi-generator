package dev.banking.asyncapi.generator.core.generator.catalog

import dev.banking.asyncapi.generator.core.generator.schema.generatedSchemaName
import dev.banking.asyncapi.generator.core.generator.schema.linkedSchema
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * The generated names of the schemas that channel messages use, in payloads and in headers.
 *
 * @param payloadSchemaNames the names reachable from a message payload.
 * @param headerSchemaNames the names reachable from message or trait headers.
 */
internal data class SchemaUsageIndex(
    val payloadSchemaNames: Set<String>,
    val headerSchemaNames: Set<String>,
) {
    /** Whether [schemaName] is used in headers and never in a payload, so it needs no source model. */
    fun isHeaderOnly(schemaName: String): Boolean = schemaName in headerSchemaNames && schemaName !in payloadSchemaNames

    companion object {
        /** The schema names [asyncApiDocument] uses in message payloads and headers. */
        fun of(asyncApiDocument: AsyncApiDocument): SchemaUsageIndex {
            val payloadNames = mutableSetOf<String>()
            val headerNames = mutableSetOf<String>()
            asyncApiDocument.channels
                ?.values
                ?.mapNotNull { it.resolvedOrNull() }
                ?.flatMap { channel -> channel.messages?.values.orEmpty() }
                ?.mapNotNull { it.resolvedOrNull() }
                ?.forEach { message ->
                    message.payload?.let { collectFromSchemaInterface(it, payloadNames, mutableSetOf()) }
                    message.headers?.let { collectFromSchemaInterface(it, headerNames, mutableSetOf()) }
                    message.traits
                        ?.mapNotNull { it.resolvedOrNull() }
                        ?.forEach { trait ->
                            trait.headers?.let { collectFromSchemaInterface(it, headerNames, mutableSetOf()) }
                        }
                }
            return SchemaUsageIndex(
                payloadSchemaNames = payloadNames,
                headerSchemaNames = headerNames,
            )
        }

        private fun collectFromSchemaInterface(
            schemaInterface: SchemaInterface,
            sink: MutableSet<String>,
            visitedRefs: MutableSet<String>,
        ) {
            when (schemaInterface) {
                is SchemaInterface.SchemaInline -> collectFromSchema(schemaInterface.schema, sink, visitedRefs)
                is SchemaInterface.SchemaReference -> {
                    if (collectFromReference(schemaInterface.reference, sink, visitedRefs)) {
                        schemaInterface.linkedSchema()?.let {
                            collectFromSchema(it, sink, visitedRefs)
                        }
                    }
                }
                else -> Unit
            }
        }

        private fun collectFromReference(
            reference: Reference,
            sink: MutableSet<String>,
            visitedRefs: MutableSet<String>,
        ): Boolean {
            val schemaName = reference.generatedSchemaName()
            val firstVisit =
                schemaName.isNotBlank() && visitedRefs.add("${reference.sourceId.orEmpty()}:${reference.ref}")
            if (firstVisit) sink.add(schemaName)
            return firstVisit
        }

        private fun collectFromSchema(
            schema: Schema,
            sink: MutableSet<String>,
            visitedRefs: MutableSet<String>,
        ) {
            schema.properties?.values?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.items?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.additionalProperties?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.oneOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.anyOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.allOf?.forEach { collectFromSchemaInterface(it, sink, visitedRefs) }
            schema.not?.let { collectFromSchemaInterface(it, sink, visitedRefs) }
        }
    }
}
