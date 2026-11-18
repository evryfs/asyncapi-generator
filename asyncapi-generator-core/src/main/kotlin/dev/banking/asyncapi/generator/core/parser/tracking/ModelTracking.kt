package dev.banking.asyncapi.generator.core.parser.tracking

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiParserProfile
import dev.banking.asyncapi.generator.core.source.DocumentNode
import dev.banking.asyncapi.generator.core.source.DocumentObject
import dev.banking.asyncapi.generator.core.source.SourceId
import dev.banking.asyncapi.generator.core.source.SourceLocation
import java.io.File
import java.util.IdentityHashMap
import kotlin.reflect.KProperty0

/**
 * The document node every parsed model was built from.
 *
 * Validators use it to find the source location of a model or of one of its fields, and the
 * loader uses it to find the model a reference points at.
 *
 * @param sources the source documents the nodes belong to
 * @param locations the source locations of the nodes
 */
internal class ModelTracking(
    private val sources: SourceTracking,
    private val locations: NodeLocations,
) {
    /**
     * Where a reference was written.
     *
     * @property sourceId the document that contains the reference
     * @property file the file of that document
     * @property profile the parser profile in effect at the reference, applied to the fragment it loads
     */
    data class ReferenceOrigin(
        val sourceId: SourceId,
        val file: File,
        val profile: AsyncApiParserProfile?,
    )

    /**
     * The node a model was built from.
     *
     * @property address the node's address
     * @property node the node's value
     */
    private class Origin(
        val address: NodeAddress,
        val node: DocumentNode,
    )

    private val origins = IdentityHashMap<Any, Origin>()
    private val modelsByAddress = mutableMapOf<NodeAddress, Any>()
    private val referenceOrigins = IdentityHashMap<Reference, ReferenceOrigin>()

    /** Records that [model] was built from [node]. */
    fun register(
        model: Any,
        node: ParserNode,
    ) {
        val address = node.address
        origins[model] = Origin(address, node.node)
        modelsByAddress[address] = model
        if (model is Reference) {
            model.sourceId = address.sourceId.value
            sources.file(address.sourceId)?.let { file ->
                referenceOrigins[model] = ReferenceOrigin(address.sourceId, file, node.profile)
            }
        }
    }

    /** The location of [model], or of its closest registered ancestor node. */
    fun getSourceLocation(model: Any): SourceLocation? = origins[model]?.address?.let(locations::nearestLocation)

    /**
     * The location of the member at [fieldPath] below [model], or of its closest ancestor that is
     * written in the source.
     */
    fun getSourceLocation(
        model: Any,
        vararg fieldPath: String,
    ): SourceLocation? {
        val address = origins[model]?.address ?: return null
        return locations.nearestLocation(fieldPath.fold(address, NodeAddress::member))
    }

    /** The location of element [index] of the array [model] was built from, or of [model] itself when absent. */
    fun getElementLocation(
        model: Any,
        index: Int,
    ): SourceLocation? {
        val address = origins[model]?.address ?: return null
        return locations.location(address.index(index)) ?: locations.nearestLocation(address)
    }

    /** The location of the member named like [property] of [model], or of [model] itself when it is absent. */
    fun <R> getSourceLocation(
        model: Any,
        property: KProperty0<R>,
    ): SourceLocation? = getSourceLocation(model, property.name)

    /** The member names written in the source object of [model]; empty when it was not an object. */
    fun getFieldNames(model: Any): Set<String> = (origins[model]?.node as? DocumentObject)?.members?.keys.orEmpty()

    /** The model registered at [address], or `null` when there is none. */
    fun modelAt(address: NodeAddress): Any? = modelsByAddress[address]

    /** Where [reference] was written, or `null` when it was not registered. */
    fun getReferenceOrigin(reference: Reference): ReferenceOrigin? = referenceOrigins[reference]

    /** The model [reference] points at, or `null` when its target is not a registered model. */
    fun findReference(reference: Reference): Any? = targetAddress(reference)?.let(modelsByAddress::get)

    private fun targetAddress(reference: Reference): NodeAddress? {
        val sourceId = referenceOrigins[reference]?.sourceId ?: reference.sourceId?.let(::SourceId)
        val sourceFile = sourceId?.let(sources::file)
        val parsed = runCatching { reference.ref.parseReference() }.getOrNull()
        if (sourceId == null || sourceFile == null || parsed == null) return null
        val targetFile = parsed.resolveDocumentAgainst(sourceFile)
        val targetSourceId = if (targetFile == null) sourceId else sources.sourceId(targetFile)
        return targetSourceId?.let { locations.resolveAddress(it, parsed.pointerSegments()) }
    }
}
