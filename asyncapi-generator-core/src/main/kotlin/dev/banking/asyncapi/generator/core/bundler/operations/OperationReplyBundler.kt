package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface

/**
 * Bundles operation reply objects and references.
 */
internal class OperationReplyBundler {
    private val operationReplyAddressBundler = OperationReplyAddressBundler()

    fun bundleMap(
        replies: Map<String, OperationReplyInterface>?,
        scope: BundlingScope,
    ): Map<String, OperationReplyInterface>? = replies?.mapValues { (_, reply) -> bundle(reply, scope) }

    fun bundle(
        replyInterface: OperationReplyInterface,
        scope: BundlingScope,
    ): OperationReplyInterface =
        when (replyInterface) {
            is OperationReplyInterface.OperationReplyInline ->
                OperationReplyInterface.OperationReplyInline(
                    bundleReply(replyInterface.operationReply, scope),
                )

            is OperationReplyInterface.OperationReplyReference -> {
                replyInterface.copy(
                    reference =
                        ReferenceBundler.bundleReferencedModel<OperationReply>(
                            reference = replyInterface.reference,
                            scope = scope,
                        ) { reply, nextScope ->
                            bundleReply(reply, nextScope)
                        },
                )
            }
        }

    private fun bundleReply(
        reply: OperationReply,
        scope: BundlingScope,
    ): OperationReply {
        val bundledAddress = reply.address?.let { operationReplyAddressBundler.bundle(it, scope) }
        // The reply channel and messages point into root channels, which are bundled themselves; like an
        // operation's channel and messages they stay references.
        return reply.copy(address = bundledAddress)
    }
}
