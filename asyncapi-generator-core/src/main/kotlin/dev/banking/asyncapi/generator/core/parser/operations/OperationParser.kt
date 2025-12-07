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
import dev.banking.asyncapi.generator.core.parser.references.ReferenceParser

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
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            val reference = node.optional($$"$ref")?.coerce<String>()
            val operationInterface = if (reference != null) {
                OperationInterface.OperationReference(
                    Reference(
                        ref = reference,
                    ).also { asyncApiContext.register(it, node) }
                )
            } else {
                OperationInterface.OperationInline(
                    Operation(
                        title = node.optional("title")?.coerce<String>(),
                        summary = node.optional("summary")?.coerce<String>(),
                        description = node.optional("description")?.coerce<String>(),
                        action = node.mandatory("action").coerce<String>(),
                        channel = node.optional("channel")?.let(referenceParser::parseElement),
                        messages = node.optional("messages")?.let(referenceParser::parseList),
                        bindings = node.optional("bindings")?.let(bindingParser::parseMap),
                        traits = node.optional("traits")?.let(operationTraitParser::parseList),
                        tags = node.optional("tags")?.let(tagParser::parseList),
                        externalDocs = node.optional("externalDocs")?.let(externalDocsParser::parseElement),
                        reply = node.optional("reply")?.let(operationReplyParser::parseElement),
                        security = node.optional("security")?.let(securitySchemeParser::parseList),
                    ).also { asyncApiContext.register(it, node) }
                )
            }
            put(node.name, operationInterface)
        }
    }
}
