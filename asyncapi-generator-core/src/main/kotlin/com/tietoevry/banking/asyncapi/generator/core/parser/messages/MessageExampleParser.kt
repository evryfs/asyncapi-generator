package com.tietoevry.banking.asyncapi.generator.core.parser.messages

import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageExample
import com.tietoevry.banking.asyncapi.generator.core.parser.node.ParserNode
import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext

class MessageExampleParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseList(parserNode: ParserNode): List<MessageExample> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val messageExample = MessageExample(
                headers = node.optional("headers")?.coerce<Map<String, Any?>>(),
                payload = node.optional("payload")?.coerce<Any?>(),
                name = node.optional("name")?.coerce<String>(),
                summary = node.optional("summary")?.coerce<String>()
            ).also { asyncApiContext.register(it, node) }
            add(messageExample)
        }
    }
}
