package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class OperationTraitParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationTraitInterface> = buildMap {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<OperationTraitInterface> = buildList {
        val nodes = parserNode.extractNodes()
        nodes.forEach { node ->
            node.coerce<Map<*, *>>()
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationTraitInterface {
        val reference = parserNode.optional($$"$ref")?.coerce<String>()
        val operationTraitInterface = if (reference != null) {
            OperationTraitInterface.OperationTraitReference(
                Reference(
                    ref = reference,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            OperationTraitInterface.OperationTraitInline(
                OperationTrait(
                    title = parserNode.optional("title")?.coerce<String>(),
                    summary = parserNode.optional("summary")?.coerce<String>(),
                    description = parserNode.optional("description")?.coerce<String>(),
                    tags = parserNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = parserNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = parserNode.optional("bindings")?.let(bindingParser::parseMap),
                    security = parserNode.optional("security")?.let(securitySchemeParser::parseMap),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return operationTraitInterface
    }
}
