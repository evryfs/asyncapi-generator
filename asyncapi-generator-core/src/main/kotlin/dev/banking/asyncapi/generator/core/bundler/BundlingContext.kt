package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.schemas.PromotedSchemaRegistry
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaRecursionAnalyzer
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Carries traversal state while the bundler walks an AsyncAPI document.
 *
 * The context keeps reference-visit tracking in one dedicated model instead of
 * passing bare sets through every bundler. This makes circular-reference
 * handling explicit while object-specific bundlers still control their own
 * traversal rules.
 *
 * Expected behavior is covered by:
 * - `BundlingContextTest`
 */
class BundlingContext private constructor(
    private val visitedReferences: Set<ReferenceIdentity>,
    internal val schemaPromotions: PromotedSchemaRegistry,
    internal val schemaRecursion: SchemaRecursionAnalyzer,
    internal val externalSchemaScope: Boolean,
    private val rootSchemaDefinition: String?,
) {

    private data class ReferenceIdentity(
        val sourceId: String?,
        val ref: String,
    )

    fun hasVisited(reference: Reference): Boolean =
        ReferenceIdentity(reference.sourceId, reference.ref) in visitedReferences

    fun hasVisited(reference: String): Boolean =
        ReferenceIdentity(null, reference) in visitedReferences

    fun enter(reference: Reference): BundlingContext =
        copy(visitedReferences = visitedReferences + ReferenceIdentity(reference.sourceId, reference.ref))

    fun enter(reference: String): BundlingContext =
        copy(visitedReferences = visitedReferences + ReferenceIdentity(null, reference))

    internal fun enterExternalSchema(reference: Reference): BundlingContext =
        copy(
            visitedReferences = visitedReferences + ReferenceIdentity(reference.sourceId, reference.ref),
            externalSchemaScope = true,
            rootSchemaDefinition = null,
        )

    internal fun defineRootSchema(name: String): BundlingContext =
        copy(rootSchemaDefinition = name)

    internal fun definesRootSchema(name: String): Boolean =
        rootSchemaDefinition == name

    private fun copy(
        visitedReferences: Set<ReferenceIdentity> = this.visitedReferences,
        externalSchemaScope: Boolean = this.externalSchemaScope,
        rootSchemaDefinition: String? = this.rootSchemaDefinition,
    ): BundlingContext =
        BundlingContext(
            visitedReferences = visitedReferences,
            schemaPromotions = schemaPromotions,
            schemaRecursion = schemaRecursion,
            externalSchemaScope = externalSchemaScope,
            rootSchemaDefinition = rootSchemaDefinition,
        )

    companion object {
        fun empty(): BundlingContext = withRootSchemas(emptyMap())

        internal fun withRootSchemas(rootSchemas: Map<String, SchemaInterface>): BundlingContext =
            BundlingContext(
                visitedReferences = emptySet(),
                schemaPromotions = PromotedSchemaRegistry(rootSchemas),
                schemaRecursion = SchemaRecursionAnalyzer(),
                externalSchemaScope = false,
                rootSchemaDefinition = null,
            )

        fun from(visitedReferences: Set<String>): BundlingContext =
            empty().copy(
                visitedReferences = visitedReferences.mapTo(linkedSetOf()) { reference ->
                    ReferenceIdentity(null, reference)
                },
            )
    }
}
