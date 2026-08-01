package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser

/**
 * Parses AsyncAPI operation objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `OperationParserTest`
 */
class OperationParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationTraitParser = OperationTraitParser(asyncApiContext)
    private val operationReplyParser = OperationReplyParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationInterface> = buildMap {
        parserNode.members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationInterface {
        val reference = parserNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            OperationInterface.OperationReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = OPERATION,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            OperationInterface.OperationInline(
                Operation(
                    title = parserNode.optional("title")?.expect<String>(),
                    summary = parserNode.optional("summary")?.expect<String>(),
                    description = parserNode.optional("description")?.expect<String>(),
                    action = parserNode.required("action").expect<String>(),
                    channel = parserNode.optional("channel")?.let { referenceParser.parseElement(it, CHANNEL) },
                    messages = parserNode.optional("messages")?.let { referenceParser.parseList(it, MESSAGE) },
                    bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap),
                    traits = parserNode.optional("traits")?.let(operationTraitParser::parseList),
                    tags = parserNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    reply = parserNode.optional("reply")?.let(operationReplyParser::parseElement),
                    security = parserNode.optional("security")?.let(securitySchemeParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
