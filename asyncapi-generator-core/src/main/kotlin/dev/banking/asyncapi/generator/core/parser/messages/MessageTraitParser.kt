package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.MESSAGE
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.correlations.CorrelationIdParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.MESSAGE_TRAIT as MESSAGE_TRAIT_OBJECT

/**
 * Parses AsyncAPI message trait objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class MessageTraitParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser = TagParser()
    private val schemaParser = SchemaParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser()
    private val correlationIdParser = CorrelationIdParser()
    private val messageExampleParser = MessageExampleParser()

    fun parseElement(parserNode: ParserNode): MessageTraitInterface =
        parserNode.parseReferenceable(
            category = MESSAGE_TRAIT,
            objectType = MESSAGE_TRAIT_OBJECT,
            reference = MessageTraitInterface::MessageTraitReference,
            inline = MessageTraitInterface::MessageTraitInline,
        ) { objectNode ->
            MessageTrait(
                headers = objectNode.optional("headers")?.let(schemaParser::parseElement),
                correlationId = objectNode.optional("correlationId")?.let(correlationIdParser::parseElement),
                contentType = objectNode.optional("contentType")?.expect<String>(),
                name = objectNode.optional("name")?.expect<String>(),
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, MESSAGE) },
                examples = objectNode.optional("examples")?.parseList(messageExampleParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
