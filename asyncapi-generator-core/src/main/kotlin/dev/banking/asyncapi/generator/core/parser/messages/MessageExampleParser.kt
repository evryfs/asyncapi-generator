package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

/**
 * Parses AsyncAPI message example objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `MessageExampleParserTest`
 * - `MessageParserTest`
 */
class MessageExampleParser(
    val asyncApiContext: AsyncApiContext,
) {

    fun parseList(parserNode: ParserNode): List<MessageExample> = buildList {
        parserNode.expectArray().elements().forEach { node ->
            val objectNode = node.expectObject()
            val messageExample = MessageExample(
                headers = objectNode.optional("headers")?.expect<Map<String, Any?>>(),
                payload = objectNode.optional("payload")?.toPlainValue(),
                name = objectNode.optional("name")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>()
            ).also { asyncApiContext.register(it, node) }
            add(messageExample)
        }
    }
}
