package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation.OPERATION
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.parser.bindings.BindingParser
import dev.banking.asyncapi.generator.core.parser.externaldocs.ExternalDocsParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseList
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.security.SecuritySchemeParser
import dev.banking.asyncapi.generator.core.parser.tags.TagParser
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.OPERATION_TRAIT as OPERATION_TRAIT_OBJECT

/**
 * Parses AsyncAPI operation trait objects from parser nodes.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class OperationTraitParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val tagParser = TagParser()
    private val bindingParser = BindingParser(asyncApiContext)
    private val externalDocsParser = ExternalDocsParser()
    private val securitySchemeParser = SecuritySchemeParser()

    fun parseElement(parserNode: ParserNode): OperationTraitInterface =
        parserNode.parseReferenceable(
            category = OPERATION_TRAIT,
            objectType = OPERATION_TRAIT_OBJECT,
            reference = OperationTraitInterface::OperationTraitReference,
            inline = OperationTraitInterface::OperationTraitInline,
        ) { objectNode ->
            OperationTrait(
                title = objectNode.optional("title")?.expect<String>(),
                summary = objectNode.optional("summary")?.expect<String>(),
                description = objectNode.optional("description")?.expect<String>(),
                tags = objectNode.optional("tags")?.parseList(tagParser::parseElement),
                externalDocs = objectNode.optional("externalDocs")?.let(externalDocsParser::parseElement),
                bindings = objectNode.optional("bindings")?.let { bindingParser.parseMap(it, OPERATION) },
                security = objectNode.optional("security")?.parseList(securitySchemeParser::parseElement),
                extensions = objectNode.extensions(),
            )
        }
}
