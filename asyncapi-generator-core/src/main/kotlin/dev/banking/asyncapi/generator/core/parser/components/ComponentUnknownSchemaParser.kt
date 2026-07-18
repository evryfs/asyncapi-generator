package dev.banking.asyncapi.generator.core.parser.components


import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.context.AsyncApiContext

class ComponentUnknownSchemaParser(
    val asyncApiContext: AsyncApiContext,
) {
    private val schemaParser = SchemaParser(asyncApiContext)

    fun parseElement(node: ParserNode): ComponentInterface =
        ComponentInterface.ComponentInline(
            Component(
                schemas = node.optional("schemas")?.let(schemaParser::parseMap),
            ).also { asyncApiContext.register(it, node) }
        )
}