package dev.banking.asyncapi.generator.core.parser.parameters

import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.parseReferenceable
import dev.banking.asyncapi.generator.core.parser.version.AsyncApiObjectType.PARAMETER as PARAMETER_OBJECT

/**
 * Parses AsyncAPI parameter objects from parser nodes.
 */
internal class ParameterParser {
    fun parseElement(parserNode: ParserNode): ParameterInterface =
        parserNode.parseReferenceable(
            category = PARAMETER,
            objectType = PARAMETER_OBJECT,
            reference = ParameterInterface::ParameterReference,
            inline = ParameterInterface::ParameterInline,
        ) { objectNode ->
            Parameter(
                description = objectNode.optional("description")?.expect<String>(),
                location = objectNode.optional("location")?.expect<String>(),
                enum = objectNode.optional("enum")?.expect<List<String>>(),
                default = objectNode.optional("default")?.expect<String>(),
                examples = objectNode.optional("examples")?.expect<List<String>>(),
                extensions = objectNode.extensions(),
            )
        }
}
