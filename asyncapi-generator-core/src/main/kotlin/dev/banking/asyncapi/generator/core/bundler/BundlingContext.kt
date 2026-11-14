package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.schemas.PromotedSchemaRegistry
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaRecursionAnalyzer
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.isExternalReference
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Carries traversal state while the bundler walks an AsyncAPI document.
 *
 * The context keeps reference-visit tracking in one dedicated model instead of
 * passing bare sets through every bundler. This makes circular-reference
 * handling explicit while object-specific bundlers still control their own
 * traversal rules.
 */
internal class BundlingContext private constructor(
    private val visitedReferences: Set<ReferenceIdentity>,
    internal val schemaPromotions: PromotedSchemaRegistry,
    internal val schemaRecursion: SchemaRecursionAnalyzer,
    /** Whether traversal is inside a document other than the root, where `#/...` points into that document. */
    internal val externalDocumentScope: Boolean,
    private val rootSchemaDefinition: String?,
) {

    private data class ReferenceIdentity(
        val sourceId: String?,
        val ref: String,
    )

    fun hasVisited(reference: Reference): Boolean =
        ReferenceIdentity(reference.sourceId, reference.ref) in visitedReferences

    /** Follows [reference]; an external reference moves traversal into another document. */
    fun enter(reference: Reference): BundlingContext =
        copy(
            visitedReferences = visitedReferences + ReferenceIdentity(reference.sourceId, reference.ref),
            externalDocumentScope = externalDocumentScope || reference.ref.isExternalReference(),
        )

    internal fun enterExternalSchema(reference: Reference): BundlingContext =
        copy(
            visitedReferences = visitedReferences + ReferenceIdentity(reference.sourceId, reference.ref),
            externalDocumentScope = true,
            rootSchemaDefinition = null,
        )

    internal fun defineRootSchema(name: String): BundlingContext =
        copy(rootSchemaDefinition = name)

    internal fun definesRootSchema(name: String): Boolean =
        rootSchemaDefinition == name

    private fun copy(
        visitedReferences: Set<ReferenceIdentity> = this.visitedReferences,
        externalDocumentScope: Boolean = this.externalDocumentScope,
        rootSchemaDefinition: String? = this.rootSchemaDefinition,
    ): BundlingContext =
        BundlingContext(
            visitedReferences = visitedReferences,
            schemaPromotions = schemaPromotions,
            schemaRecursion = schemaRecursion,
            externalDocumentScope = externalDocumentScope,
            rootSchemaDefinition = rootSchemaDefinition,
        )

    companion object {
        internal fun empty(): BundlingContext = withRootSchemas(emptyMap())

        internal fun withRootSchemas(rootSchemas: Map<String, SchemaInterface>): BundlingContext =
            BundlingContext(
                visitedReferences = emptySet(),
                schemaPromotions = PromotedSchemaRegistry(rootSchemas),
                schemaRecursion = SchemaRecursionAnalyzer(),
                externalDocumentScope = false,
                rootSchemaDefinition = null,
            )

    }
}
