package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface

/**
 * Bundles operation reply address objects and references.
 */
internal class OperationReplyAddressBundler {
    fun bundleMap(
        addresses: Map<String, OperationReplyAddressInterface>?,
        scope: BundlingScope,
    ): Map<String, OperationReplyAddressInterface>? = addresses?.mapValues { (_, addr) -> bundle(addr, scope) }

    fun bundle(
        address: OperationReplyAddressInterface,
        scope: BundlingScope,
    ): OperationReplyAddressInterface =
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline -> address
            is OperationReplyAddressInterface.OperationReplyAddressReference -> {
                address.copy(reference = ReferenceBundler.inlineIfUnvisited(address.reference, scope))
            }
        }
}
