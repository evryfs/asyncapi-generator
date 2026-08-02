package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT

/**
 * Parses AsyncAPI operation trait objects from parser nodes.
 *
 * Expected behavior is covered by:
 * - `OperationTraitParserTest`
 * - `OperationParserTest`
 */
class OperationTraitParser(
    val asyncApiContext: AsyncApiContext,
) {

    private val tagParser = TagParser(asyncApiContext)
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser(asyncApiContext)
    private val securitySchemeParser = SecuritySchemeParser(asyncApiContext)

    fun parseMap(parserNode: ParserNode): Map<String, OperationTraitInterface> = buildMap {
        parserNode.expectObject().members().forEach { node ->
            put(node.name, parseElement(node))
        }
    }

    fun parseList(parserNode: ParserNode): List<OperationTraitInterface> = buildList {
        parserNode.expectArray().elements().forEach { node ->
            add(parseElement(node))
        }
    }

    fun parseElement(parserNode: ParserNode): OperationTraitInterface {
        val objectNode = parserNode.expectObject()
        val reference = objectNode.optional($$"$ref")?.expect<String>()
        val operationTraitInterface = if (reference != null) {
            OperationTraitInterface.OperationTraitReference(
                Reference(
                    ref = reference,
                    referenceCategoryKey = OPERATION_TRAIT,
                ).also { asyncApiContext.register(it, parserNode) }
            )
        } else {
            OperationTraitInterface.OperationTraitInline(
                OperationTrait(
                    title = objectNode.optional("title")?.expect<String>(),
                    summary = objectNode.optional("summary")?.expect<String>(),
                    description = objectNode.optional("description")?.expect<String>(),
                    tags = objectNode.optional("tags")?.let(tagParser::parseList),
                    externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                    bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, OPERATION) },
                    security = objectNode.optional("security")?.let(securitySchemeParser::parseList),
                ).also { asyncApiContext.register(it, parserNode) }
            )
        }
        return operationTraitInterface
    }
}
