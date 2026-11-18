package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION as OPERATION_BINDING
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION as OPERATION_OBJECT

/**
 * Parses AsyncAPI operation objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class OperationParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser = TagParser()
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationTraitParser = OperationTraitParser(asyncApiContext)
    private val operationReplyParser = OperationReplyParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser()
    private val externalDocsParser = ExternalDocsParser()

    fun parseElement(parserNode: ParserNode): OperationInterface =
        parserNode.parseReferenceable(
            category = OPERATION,
            objectType = OPERATION_OBJECT,
            reference = OperationInterface::OperationReference,
            inline = OperationInterface::OperationInline,
        ) { objectNode ->
            Operation(
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                action = objectNode.required("action").expect<String>(),
                channel = objectNode.required("channel").let(referenceParser::parseChannel),
                messages = objectNode.optional("messages")?.let(referenceParser::parseMessages),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, OPERATION_BINDING) },
                traits = objectNode.optional("traits")?.parseList(operationTraitParser::parseElement),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                reply = objectNode.optional("reply")?.let(operationReplyParser::parseElement),
                security = objectNode.optional("security")?.parseList(securitySchemeParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
