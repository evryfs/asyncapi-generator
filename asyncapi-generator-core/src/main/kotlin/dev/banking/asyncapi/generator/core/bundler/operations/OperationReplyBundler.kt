package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
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
        context: BundlingContext,
    ): Map<String, OperationReplyInterface>? =
        replies?.mapValues { (_, reply) -> bundle(reply, context) }
    fun bundle(replyInterface: OperationReplyInterface, context: BundlingContext): OperationReplyInterface =
        when (replyInterface) {
            is OperationReplyInterface.OperationReplyInline ->
                OperationReplyInterface.OperationReplyInline(
                    bundleReply(replyInterface.operationReply, context)
                )

            is OperationReplyInterface.OperationReplyReference -> {
                replyInterface.copy(reference = ReferenceBundler.bundleReferencedModel<OperationReply>(
                    reference = replyInterface.reference,
                    context = context,
                ) { reply, nextContext ->
                    bundleReply(reply, nextContext)
                })
            }
        }

    private fun bundleReply(reply: OperationReply, context: BundlingContext): OperationReply {
        val bundledAddress = reply.address?.let { operationReplyAddressBundler.bundle(it, context) }
        val bundledChannel = reply.channel?.let { ReferenceBundler.inlineIfUnvisited(it, context) }
        val bundledMessages = reply.messages?.map { ReferenceBundler.inlineIfUnvisited(it, context) }

        return reply.copy(
            address = bundledAddress,
            channel = bundledChannel,
            messages = bundledMessages,
        )
    }
}
