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
        parserNode.elements().forEach { node ->
            val messageExample = MessageExample(
                headers = node.optional("headers")?.expect<Map<String, Any?>>(),
                payload = node.optional("payload")?.toPlainValue(),
                name = node.optional("name")?.expect<String>(),
                summary = node.optional("summary")?.expect<String>()
            ).also { asyncApiContext.register(it, node) }
            add(messageExample)
        }
    }
}
