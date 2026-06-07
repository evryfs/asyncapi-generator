package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface

/**
 * Bundles operation reply address objects and references.
 *
 * Expected behavior is covered by:
 * - `OperationReplyAddressBundlerTest`
 */
class OperationReplyAddressBundler {

    fun bundleMap(
        addresses: Map<String, OperationReplyAddressInterface>?,
        visited: Set<String>
    ): Map<String, OperationReplyAddressInterface>? =
        bundleMap(addresses, BundlingContext.from(visited))

    fun bundleMap(
        addresses: Map<String, OperationReplyAddressInterface>?,
        context: BundlingContext,
    ): Map<String, OperationReplyAddressInterface>? =
        addresses?.mapValues { (_, addr) -> bundle(addr, context) }

    fun bundle(address: OperationReplyAddressInterface, visited: Set<String>): OperationReplyAddressInterface =
        bundle(address, BundlingContext.from(visited))

    fun bundle(address: OperationReplyAddressInterface, context: BundlingContext): OperationReplyAddressInterface =
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline -> address
            is OperationReplyAddressInterface.OperationReplyAddressReference -> {
                ReferenceBundler.inlineIfUnvisited(address.reference, context)
                address
            }
        }
}
