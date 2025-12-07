package com.tietoevry.banking.asyncapi.generator.core.bundler.operations

import com.tietoevry.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface

class OperationReplyAddressBundler {

    fun bundleMap(
        addresses: Map<String, OperationReplyAddressInterface>?,
        visited: Set<String>
    ): Map<String, OperationReplyAddressInterface>? =
        addresses?.mapValues { (_, addr) -> bundle(addr, visited) }

    fun bundle(address: OperationReplyAddressInterface, visited: Set<String>): OperationReplyAddressInterface =
        when (address) {
            is OperationReplyAddressInterface.OperationReplyAddressInline -> address
            is OperationReplyAddressInterface.OperationReplyAddressReference -> {
                val ref = address.reference.ref
                if (visited.contains(ref)) {
                    address
                } else {
                    address.reference.inline()
                    address
                }
            }
        }
}
