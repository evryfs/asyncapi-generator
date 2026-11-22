package dev.banking.asyncapi.generator.core.bundler.components

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
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
        scope: BundlingScope,
    ): Component {
        val bundledSchemas = schemaBundler.bundleComponentMap(component.schemas, scope)
        val bundledServers = component.servers?.let { serverBundler.bundleServers(it, scope) }
        val bundledChannels = component.channels?.let { channelBundler.bundleMap(it, scope) }
        val bundledOperations = component.operations?.let { operationBundler.bundleMap(it, scope) }
        val bundledMessages = messagesBundler.bundleMap(component.messages, scope)
        val bundledSecuritySchemes = securitySchemeBundler.bundleMap(component.securitySchemes, scope)
        val bundledServerVariables = serverVariableBundler.bundleMap(component.serverVariables, scope)
        val bundledParameters = parameterBundler.bundleMap(component.parameters, scope)
        val bundledCorrelationIds = correlationIdBundler.bundleMap(component.correlationIds, scope)
        val bundledReplies = operationReplyBundler.bundleMap(component.replies, scope)
        val bundledReplyAddresses = operationReplyAddressBundler.bundleMap(component.replyAddresses, scope)
        val bundledExternalDocs = externalDocsBundler.bundleMap(component.externalDocs, scope)
        val bundledTags = tagBundler.bundleMap(component.tags, scope)
        val bundledOperationTraits = operationTraitBundler.bundleMap(component.operationTraits, scope)
        val bundledMessageTraits = messageTraitsBundler.bundleMap(component.messageTraits, scope)
        val bundledServerBindings = bindingBundler.bundleMap(component.serverBindings, scope)
        val bundledChannelBindings = bindingBundler.bundleMap(component.channelBindings, scope)
        val bundledOperationBindings = bindingBundler.bundleMap(component.operationBindings, scope)
        val bundledMessageBindings = bindingBundler.bundleMap(component.messageBindings, scope)
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

    internal fun mergeSchemas(
        components: Component?,
        schemas: Map<String, SchemaInterface>,
    ): Component? {
        if (schemas.isEmpty()) return components

        return components?.copy(schemas = components.schemas.orEmpty() + schemas) ?: Component(schemas = schemas)
    }
}
