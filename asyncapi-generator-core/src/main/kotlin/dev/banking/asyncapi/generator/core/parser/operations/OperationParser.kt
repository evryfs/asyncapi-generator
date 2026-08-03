package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION as OPERATION_BINDING
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION as OPERATION_OBJECT

/**
 * Parses AsyncAPI operation objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `OperationParserTest`
 */
internal class OperationParser(
    private val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val referenceParser = ReferenceParser(asyncApiContext)
    private val operationTraitParser = OperationTraitParser(asyncApiContext)
    private val operationReplyParser = OperationReplyParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationInterface> = buildMap {
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        return if (reference != null) {
            OperationInterface.OperationReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = OPERATION,
                ).also { asyncApiContext.register(it, parserNode) },
            )
        } else {
            objectNode.expectOnlyMembers(OPERATION_OBJECT)
            OperationInterface.OperationInline(
                Operation(
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    action = objectNode.required("action").expect<String>(),
                    channel = objectNode.required("channel").let { referenceParser.parseElement(it, CHANNEL) },
                    messages = objectNode.optional("messages")?.let { referenceParser.parseList(it, MESSAGE) },
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, OPERATION_BINDING) },
                    traits = objectNode.optional("traits")?.let(operationTraitParser::parseList),
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    reply = objectNode.optional("reply")?.let(operationReplyParser::parseElement),
                    security = objectNode.optional("security")?.let(securitySchemeParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) },
            )
        }
    }
}
