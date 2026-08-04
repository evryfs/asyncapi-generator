package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE_TRAIT as MESSAGE_TRAIT_OBJECT

/**
 * Parses AsyncAPI message trait objects from parser nodes.
 */
internal class MessageTraitParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val schemaParser = SchemaParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val correlationIdParser = CorrelationIdParser(asyncApiContext)
    private val messageExampleParser = MessageExampleParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, MessageTraitInterface> =
        parserNode.expectObject().members().associate { node ->
            node.name to parseElement(node)
        }

    fun parseList(parserNode: ParserNode): List<MessageTraitInterface> =
        parserNode.expectArray().elements().map(::parseElement)

    fun parseElement(parserNode: ParserNode): MessageTraitInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        val messageTraitInterface = if (reference != null) {
            MessageTraitInterface.ReferenceMessageTrait(
                Reference(
                    ref = reference,
                    referenceCategoryKey = MESSAGE_TRAIT
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            objectNode.expectOnlyMembers(MESSAGE_TRAIT_OBJECT)
            MessageTraitInterface.InlineMessageTrait(
                MessageTrait(
                    headers = objectNode.optional("headers")?.let(schemaParser::parseElement),
                    correlationId = objectNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                    contentType = objectNode.optional("contentType")?.expect<String>(),
                    name = objectNode.optional("name")?.expect<String>(),
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, MESSAGE) },
                    examples = objectNode.optional("examples")?.let(messageExampleParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return messageTraitInterface
    }
}
