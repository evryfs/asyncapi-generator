package dev.banking.asyncapi.generator.core.parser.tracking

import dev.banking.asyncapi.generator.core.parser.node.NodeAddress
import dev.banking.asyncapi.generator.core.source.SourceId
import dev.banking.asyncapi.generator.core.source.SourceLocation

/**
 * The source location of every document node of one load, indexed by [NodeAddress].
 */
internal class NodeLocations {
    private val locations = mutableMapOf<NodeAddress, SourceLocation>()
    private val addressesByDisplayPath = mutableMapOf<String, NodeAddress>()

    /** Records the [location] of the node at [address]; the location's path becomes the address's display path. */
    fun register(
        address: NodeAddress,
        location: SourceLocation,
    ) {
        val displayPath = address.displayPath
        locations[address] = location.copy(path = displayPath)
        addressesByDisplayPath[displayPath] = address
    }

    /** The location of the node at [address], or `null` when no node is registered there. */
    fun location(address: NodeAddress): SourceLocation? = locations[address]

    /** The location of the node whose display path is [displayPath], or `null` when there is none. */
    fun location(displayPath: String): SourceLocation? = address(displayPath)?.let(locations::get)

    /** The location of the node at [address] or, when it has none, of its closest registered ancestor. */
    fun nearestLocation(address: NodeAddress): SourceLocation? =
        address.ancestors().firstNotNullOfOrNull(locations::get)

    /** The address of the node whose display path is [displayPath], or `null` when there is none. */
    fun address(displayPath: String): NodeAddress? = addressesByDisplayPath[displayPath]

    /**
     * Follows JSON pointer [pointerSegments] from the root of [sourceId] to a registered node.
     *
     * A segment that is a valid array index matches an element when no member has that name.
     */
    fun resolveAddress(
        sourceId: SourceId,
        pointerSegments: List<String>,
    ): NodeAddress? =
        pointerSegments.fold(NodeAddress.root(sourceId).takeIf { it in locations }) { address, segment ->
            address?.let { child(it, segment) }
        }

    private fun child(
        address: NodeAddress,
        segment: String,
    ): NodeAddress? =
        address.member(segment).takeIf { it in locations }
            ?: segment
                .takeIf(JSON_ARRAY_INDEX::matches)
                ?.toIntOrNull()
                ?.let(address::index)
                ?.takeIf { it in locations }

    private companion object {
        val JSON_ARRAY_INDEX = Regex("""0|[1-9]\d*""")
    }
}
