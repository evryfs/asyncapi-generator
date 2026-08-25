package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.references.Reference
import java.util.IdentityHashMap

/**
 * Tracks the source location and protocol of binding references.
 */
internal class BindingReferenceRegistry {

    internal data class BindingReferenceOrigin(
        val location: BindingLocation,
        val protocol: String?,
    )

    private val origins = IdentityHashMap<Reference, BindingReferenceOrigin>()

    fun register(
        reference: Reference,
        location: BindingLocation,
        protocol: String?,
    ) {
        origins[reference] = BindingReferenceOrigin(location, protocol)
    }

    fun getOrigin(reference: Reference): BindingReferenceOrigin? = origins[reference]
}
