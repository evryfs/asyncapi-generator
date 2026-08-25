package dev.banking.asyncapi.generator.core.parser.bindings

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.DocumentBoolean
import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.bindings.BindingLocation
import dev.banking.asyncapi.generator.core.model.bindings.ProtocolBinding
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.schemas.SchemaParser

/**
 * Parses raw binding content while retaining its protocol, location, version,
 * and schema-valued fields for semantic validation.
 */
internal class BindingParser(
    private val asyncApiContext: AsyncApiContext,
) {
    private val schemaParser by lazy { SchemaParser(asyncApiContext) }

    /** Parses the protocol-keyed `bindings` property of an AsyncAPI object. */
    fun parseMap(parserNode: ParserNode, location: BindingLocation): Map<String, BindingInterface> =
        parserNode.expectObject()
            .members()
            .associate { protocolNode ->
                protocolNode.name to parseProtocol(protocolNode, location)
            }

    /** Parses a component registry whose values are complete Bindings Objects. */
    fun parseComponentMap(parserNode: ParserNode, location: BindingLocation): Map<String, BindingInterface> =
        parserNode.expectObject().members().associate { componentNode ->
            componentNode.name to parseComponent(componentNode, location)
        }

    fun parseProtocol(parserNode: ParserNode, location: BindingLocation): BindingInterface {
        parseReference(parserNode, location, parserNode.name)?.let { return it }
        val content = parserNode.expect<Map<String, Any?>>()
        val protocolBinding = parseProtocolBinding(parserNode, location, parserNode.name)
        return inlineBinding(parserNode, content, listOf(protocolBinding))
    }

    internal fun parseProtocol(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String,
    ): BindingInterface {
        parseReference(parserNode, location, protocol)?.let { return it }
        val content = parserNode.expect<Map<String, Any?>>()
        val protocolBinding = parseProtocolBinding(parserNode, location, protocol)
        return inlineBinding(parserNode, content, listOf(protocolBinding))
    }

    fun parseComponent(parserNode: ParserNode, location: BindingLocation): BindingInterface {
        parseReference(parserNode, location, null)?.let { return it }
        val content = parserNode.expect<Map<String, Any?>>()
        val protocolBindings = parserNode.expectObject().members().map { protocolNode ->
            parseProtocolBinding(protocolNode, location, protocolNode.name)
        }
        return inlineBinding(parserNode, content, protocolBindings)
    }

    private fun parseReference(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String?,
    ): BindingInterface.BindingReference? {
        val reference = parserNode.expectObject().optional($$"$ref")?.expect<String>() ?: return null
        val model = Reference(
            ref = reference,
            referenceCategoryKey = BINDING,
        )
        asyncApiContext.bindingRegistry.register(model, location, protocol)
        asyncApiContext.register(model, parserNode)
        return BindingInterface.BindingReference(
            model,
        )
    }

    private fun parseProtocolBinding(
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String,
    ): ProtocolBinding {
        val content = parserNode.toPlainValue()
        val objectNode = parserNode.takeIf { it.node is DocumentObject }?.expectObject()
        registerKafkaTopicConfiguration(content, parserNode, location, protocol)
        val schemaFields =
            if (protocol == KAFKA_PROTOCOL && objectNode != null) {
                kafkaSchemaFieldNames(location).mapNotNull { fieldName ->
                    objectNode.optional(fieldName)
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

    private fun registerKafkaTopicConfiguration(
        content: Any?,
        parserNode: ParserNode,
        location: BindingLocation,
        protocol: String,
    ) {
        if (protocol != KAFKA_PROTOCOL || location != BindingLocation.CHANNEL) return
        val properties = content as? Map<*, *> ?: return
        val topicConfiguration = properties["topicConfiguration"] as? Map<*, *> ?: return
        val topicConfigurationNode = parserNode.expectObject().optional("topicConfiguration") ?: return
        asyncApiContext.register(topicConfiguration, topicConfigurationNode)
        val cleanupPolicy = topicConfiguration["cleanup.policy"] as? List<*> ?: return
        val cleanupPolicyNode = topicConfigurationNode.expectObject().optional("cleanup.policy") ?: return
        asyncApiContext.register(cleanupPolicy, cleanupPolicyNode)
    }

    private fun inlineBinding(
        parserNode: ParserNode,
        content: Map<String, Any?>,
        protocolBindings: List<ProtocolBinding>,
    ): BindingInterface.BindingInline {
        val kafkaKeySchema = protocolBindings
            .firstOrNull { it.protocol == KAFKA_PROTOCOL }
            ?.schemaFields
            ?.get("key")
        return BindingInterface.BindingInline(
            Binding(
                content = content,
                kafkaKeySchema = kafkaKeySchema,
                protocolBindings = protocolBindings,
            ).also { asyncApiContext.register(it, parserNode) },
        )
    }

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
