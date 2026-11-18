package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseObject
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE_EXAMPLE

/**
 * Parses AsyncAPI message example objects from parser nodes.
 */
internal class MessageExampleParser {
    fun parseElement(parserNode: ParserNode): MessageExample =
        parserNode.parseObject(MESSAGE_EXAMPLE) { objectNode ->
            MessageExample(
                headers = objectNode.optional("headers")?.expect<Map<String, Any?>>(),
                payload = objectNode.optional("payload")?.toPlainValue(),
                name = objectNode.optional("name")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
            )
        }
}
