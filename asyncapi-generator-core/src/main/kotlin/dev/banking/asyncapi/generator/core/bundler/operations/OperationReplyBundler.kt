package dev.banking.asyncapi.generator.core.bundler.operations

import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface

class OperationReplyBundler {

    private val operationReplyAddressBundler = OperationReplyAddressBundler()

    fun bundleMap(
        replies: Map<String, OperationReplyInterface>?,
        visited: Set<String>,
    ): Map<String, OperationReplyInterface>? =
        replies?.mapValues { (_, reply) -> bundle(reply, visited) }

    fun bundle(replyInterface: OperationReplyInterface, visited: Set<String>): OperationReplyInterface {
        return when (replyInterface) {
            is OperationReplyInterface.OperationReplyInline ->
                OperationReplyInterface.OperationReplyInline(
                    bundleReply(replyInterface.operationReply, visited)
                )

            is OperationReplyInterface.OperationReplyReference -> {
                val ref = replyInterface.reference.ref
                if (visited.contains(ref)) {
                    replyInterface
                } else {
                    val model = replyInterface.reference.requireModel<OperationReply>()
                    val newVisited = visited + ref
                    val bundled = bundleReply(model, newVisited)
                    replyInterface.reference.model = bundled
                    replyInterface.reference.inline()
                    replyInterface
                }
            }
        }
    }

    private fun bundleReply(reply: OperationReply, visited: Set<String>): OperationReply {
        val bundledAddress = reply.address?.let { operationReplyAddressBundler.bundle(it, visited) }
        reply.channel?.let { if (!visited.contains(it.ref)) it.inline() }
        reply.messages?.forEach { if (!visited.contains(it.ref)) it.inline() }

        return reply.copy(
            address = bundledAddress
        )
    }
}
