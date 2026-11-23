package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.isExternalReference

/**
 * Where the bundler is in the document: the references followed to get here, whether that left the root
 * document, and which root component schema is being defined.
 *
 * A scope never changes; following a reference returns a new scope, so sibling paths never see each
 * other's references. Every scope belongs to one [BundlingRun].
 *
 * @param run the bundling this scope belongs to
 * @param visitedReferences the references followed to reach this scope
 * @param externalDocumentScope whether traversal is inside a document other than the root, where `#/...`
 *   points into that document
 * @param rootSchemaDefinition the name of the root component schema being defined, if any
 */
internal class BundlingScope private constructor(
    val run: BundlingRun,
    private val visitedReferences: Set<ReferenceIdentity>,
    val externalDocumentScope: Boolean,
    private val rootSchemaDefinition: String?,
) {
    private data class ReferenceIdentity(
        val sourceId: String?,
        val ref: String,
    )

    /** Whether [reference] was already followed on the way to this scope. */
    fun hasVisited(reference: Reference): Boolean = reference.identity() in visitedReferences

    /** Follows [reference]; an external reference moves traversal into another document. */
    fun enter(reference: Reference): BundlingScope =
        copy(
            visitedReferences = visitedReferences + reference.identity(),
            externalDocumentScope = externalDocumentScope || reference.ref.isExternalReference(),
        )

    /** Follows [reference] into a schema of another document, which defines no root component schema. */
    fun enterExternalSchema(reference: Reference): BundlingScope =
        copy(
            visitedReferences = visitedReferences + reference.identity(),
            externalDocumentScope = true,
            rootSchemaDefinition = null,
        )

    /** The scope that defines the root component schema [name]. */
    fun defineRootSchema(name: String): BundlingScope = copy(rootSchemaDefinition = name)

    /** Whether this scope defines the root component schema [name]. */
    fun definesRootSchema(name: String): Boolean = rootSchemaDefinition == name

    private fun Reference.identity(): ReferenceIdentity = ReferenceIdentity(sourceId, ref)

    private fun copy(
        visitedReferences: Set<ReferenceIdentity> = this.visitedReferences,
        externalDocumentScope: Boolean = this.externalDocumentScope,
        rootSchemaDefinition: String? = this.rootSchemaDefinition,
    ): BundlingScope =
        BundlingScope(
            run = run,
            visitedReferences = visitedReferences,
            externalDocumentScope = externalDocumentScope,
            rootSchemaDefinition = rootSchemaDefinition,
        )

    companion object {
        /** The scope at the root of the document of [run]. */
        fun root(run: BundlingRun): BundlingScope =
            BundlingScope(
                run = run,
                visitedReferences = emptySet(),
                externalDocumentScope = false,
                rootSchemaDefinition = null,
            )
    }
}
