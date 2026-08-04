package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE_EXAMPLE

/**
 * Parses AsyncAPI message example objects from parser nodes.
 */
internal class MessageExampleParser(
    private val asyncApiContext: AsyncApiContext,
) {

    fun parseList(parserNode: ParserNode): List<MessageExample> =
        parserNode.expectArray().elements().map { node ->
            val objectNode = node.expectObject()
            objectNode.expectOnlyMembers(MESSAGE_EXAMPLE)
            MessageExample(
                headers = objectNode.optional("headers")?.expect<Map<String, Any?>>(),
                payload = objectNode.optional("payload")?.toPlainValue(),
                name = objectNode.optional("name")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>()
            ).also { asyncApiContext.register(it, node) }
        }
}
