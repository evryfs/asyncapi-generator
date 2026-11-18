package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.referenceOrNull
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser
import dev.banking.asyncapi.generator.core.source.DocumentBoolean
import dev.banking.asyncapi.generator.core.source.DocumentObject

/**
 * Parses raw binding content while retaining its protocol, location, version,
 * and schema-valued fields for semantic validation.
 *
 * @param asyncApiContext shared context for source tracking, model registration, and diagnostics
 */
internal class BindingParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaParser by lazy { SchemaParser(asyncApiContext) }

    /** Parses the protocol-keyed `bindings` property of an AsyncAPI object. */
    fun parseMap(
        parserNode: ParserNode,
        location: BindingLocation,
    ): Map<String, BindingInterface> =
        parserNode
            .expectObject()
            .members()
            .associate { protocolNode ->
                protocolNode.name to parseProtocol(protocolNode, location)
            }

    /** Parses a component registry whose values are complete Bindings Objects. */
    fun parseComponentMap(
        parserNode: ParserNode,
        location: BindingLocation,
    ): Map<String, BindingInterface> =
        parserNode.expectObject().members().associate { componentNode ->
            componentNode.name to parseComponent(componentNode, location)
        }

    /** Parses the binding of one [protocol], which defaults to the node's member name. */
    fun parseProtocol(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String = parserNode.name,
    ): BindingInterface {
        parseReference(parserNode, location, protocol)?.let { return it }
        val content = parserNode.expect<Map<String, Any?>>()
        val protocolBinding = parseProtocolBinding(parserNode, location, protocol, content)
        return inlineBinding(parserNode, content, listOf(protocolBinding))
    }

    /** Parses a Bindings Object: the bindings of several protocols keyed by protocol. */
    fun parseComponent(
        parserNode: ParserNode,
        location: BindingLocation,
    ): BindingInterface {
        parseReference(parserNode, location, null)?.let { return it }
        val content = parserNode.expect<Map<String, Any?>>()
        val protocolBindings =
            parserNode.expectObject().members().map { protocolNode ->
                parseProtocolBinding(protocolNode, location, protocolNode.name, content[protocolNode.name])
            }
        return inlineBinding(parserNode, content, protocolBindings)
    }

    private fun parseReference(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String?,
    ): BindingInterface.BindingReference? =
        parserNode
            .referenceOrNull(BINDING) { target ->
                if (protocol != null) parseProtocol(target, location, protocol) else parseComponent(target, location)
            }?.let(BindingInterface::BindingReference)

    private fun parseProtocolBinding(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String,
        content: Any?,
    ): ProtocolBinding {
        val objectNode = parserNode.takeIf { it.node is DocumentObject }?.expectObject()
        val schemaFields =
            if (protocol == KAFKA_PROTOCOL && objectNode != null) {
                kafkaSchemaFieldNames(location)
                    .mapNotNull { fieldName ->
                        objectNode
                            .optional(fieldName)
                            ?.takeIf { it.node is DocumentObject || it.node is DocumentBoolean }
                            ?.let { fieldName to schemaParser.parseElement(it) }
                    }.toMap()
            } else {
                emptyMap()
            }
        return ProtocolBinding(
            protocol = protocol,
            location = location,
            content = content,
            bindingVersion = objectNode?.optional("bindingVersion")?.toPlainValue(),
            schemaFields = schemaFields,
        ).also { asyncApiContext.register(it, parserNode) }
    }

    private fun inlineBinding(
        parserNode: ParserNode,
        content: Map<String, Any?>,
        protocolBindings: List<ProtocolBinding>,
    ): BindingInterface.BindingInline =
        BindingInterface.BindingInline(
            Binding(content = content, protocolBindings = protocolBindings)
                .also { asyncApiContext.register(it, parserNode) },
        )

    private fun kafkaSchemaFieldNames(location: BindingLocation): Set<String> =
        when (location) {
            BindingLocation.OPERATION -> setOf("groupId", "clientId")
            BindingLocation.MESSAGE -> setOf("key")
            else -> emptySet()
        }

    private companion object {
        const val KAFKA_PROTOCOL = "kafka"
    }
}
