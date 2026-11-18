package dev.banking.asyncapi.generator.core.bundler.components

import dev.banking.asyncapi.generator.core.bundler.BundlingContext
import dev.banking.asyncapi.generator.core.bundler.ReferenceBundler
import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import dev.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import dev.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import dev.banking.asyncapi.generator.core.bundler.messages.MessageTraitBundler
import dev.banking.asyncapi.generator.core.bundler.messages.MessagesBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationReplyAddressBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationReplyBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationTraitBundler
import dev.banking.asyncapi.generator.core.bundler.parameters.ParameterBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import dev.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import dev.banking.asyncapi.generator.core.bundler.servers.ServerVariableBundler
import dev.banking.asyncapi.generator.core.bundler.tags.TagBundler
import dev.banking.asyncapi.generator.core.bundler.withModel
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles component objects and references.
 */
internal class ComponentBundler {
    private val schemaBundler = SchemaBundler()
    private val serverBundler = ServerBundler()
    private val channelBundler = ChannelBundler()
    private val operationBundler = OperationBundler()
    private val messagesBundler = MessagesBundler()
    private val messageTraitsBundler = MessageTraitBundler()
    private val securitySchemeBundler = SecuritySchemeBundler()
    private val parameterBundler = ParameterBundler()
    private val correlationIdBundler = CorrelationIdBundler()
    private val externalDocsBundler = ExternalDocsBundler()
    private val tagBundler = TagBundler()
    private val operationReplyBundler = OperationReplyBundler()
    private val operationTraitBundler = OperationTraitBundler()
    private val operationReplyAddressBundler = OperationReplyAddressBundler()
    private val bindingBundler = BindingBundler()
    private val serverVariableBundler = ServerVariableBundler()

    fun bundleComponent(
        component: Component,
        context: BundlingContext,
    ): Component {
        val bundledSchemas = schemaBundler.bundleComponentMap(component.schemas, context)
        val bundledServers = component.servers?.let { serverBundler.bundleServers(it, context) }
        val bundledChannels = component.channels?.let { channelBundler.bundleMap(it, context) }
        val bundledOperations = component.operations?.let { operationBundler.bundleMap(it, context) }
        val bundledMessages = messagesBundler.bundleMap(component.messages, context)
        val bundledSecuritySchemes = securitySchemeBundler.bundleMap(component.securitySchemes, context)
        val bundledServerVariables = serverVariableBundler.bundleMap(component.serverVariables, context)
        val bundledParameters = parameterBundler.bundleMap(component.parameters, context)
        val bundledCorrelationIds = correlationIdBundler.bundleMap(component.correlationIds, context)
        val bundledReplies = operationReplyBundler.bundleMap(component.replies, context)
        val bundledReplyAddresses = operationReplyAddressBundler.bundleMap(component.replyAddresses, context)
        val bundledExternalDocs = externalDocsBundler.bundleMap(component.externalDocs, context)
        val bundledTags = tagBundler.bundleMap(component.tags, context)
        val bundledOperationTraits = operationTraitBundler.bundleMap(component.operationTraits, context)
        val bundledMessageTraits = messageTraitsBundler.bundleMap(component.messageTraits, context)
        val bundledServerBindings = bindingBundler.bundleMap(component.serverBindings, context)
        val bundledChannelBindings = bindingBundler.bundleMap(component.channelBindings, context)
        val bundledOperationBindings = bindingBundler.bundleMap(component.operationBindings, context)
        val bundledMessageBindings = bindingBundler.bundleMap(component.messageBindings, context)
        return component.copy(
            schemas = bundledSchemas,
            servers = bundledServers,
            channels = bundledChannels,
            operations = bundledOperations,
            messages = bundledMessages,
            securitySchemes = bundledSecuritySchemes,
            serverVariables = bundledServerVariables,
            parameters = bundledParameters,
            correlationIds = bundledCorrelationIds,
            replies = bundledReplies,
            replyAddresses = bundledReplyAddresses,
            externalDocs = bundledExternalDocs,
            tags = bundledTags,
            operationTraits = bundledOperationTraits,
            messageTraits = bundledMessageTraits,
            serverBindings = bundledServerBindings,
            channelBindings = bundledChannelBindings,
            operationBindings = bundledOperationBindings,
            messageBindings = bundledMessageBindings,
        )
    }

    internal fun schemas(components: Component?): Map<String, SchemaInterface> = components?.schemas.orEmpty()

    internal fun mergeSchemas(
        components: Component?,
        schemas: Map<String, SchemaInterface>,
    ): Component? {
        if (schemas.isEmpty()) return components

        return components?.copy(schemas = components.schemas.orEmpty() + schemas) ?: Component(schemas = schemas)
    }
}
