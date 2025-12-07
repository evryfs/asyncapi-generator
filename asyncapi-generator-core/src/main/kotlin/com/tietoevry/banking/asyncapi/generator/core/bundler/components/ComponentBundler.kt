package com.tietoevry.banking.asyncapi.generator.core.bundler.components

import com.tietoevry.banking.asyncapi.generator.core.bundler.correlations.CorrelationIdBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.externaldocs.ExternalDocsBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.parameters.ParameterBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.messages.MessageTraitBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.messages.MessagesBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.operations.OperationReplyAddressBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.operations.OperationReplyBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.operations.OperationTraitBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.security.SecuritySchemeBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.servers.ServerVariableBundler
import com.tietoevry.banking.asyncapi.generator.core.bundler.tags.TagBundler
import com.tietoevry.banking.asyncapi.generator.core.model.components.Component
import com.tietoevry.banking.asyncapi.generator.core.model.components.ComponentInterface

class ComponentBundler {

    private val schemaBundler: SchemaBundler = SchemaBundler()
    private val serverBundler: ServerBundler = ServerBundler()
    private val channelBundler: ChannelBundler = ChannelBundler()
    private val operationBundler: OperationBundler = OperationBundler()
    private val messagesBundler: MessagesBundler = MessagesBundler()
    private val messageTraitsBundler = MessageTraitBundler()
    private val securitySchemeBundler: SecuritySchemeBundler = SecuritySchemeBundler()
    private val parameterBundler: ParameterBundler = ParameterBundler()
    private val correlationIdBundler: CorrelationIdBundler = CorrelationIdBundler()
    private val externalDocsBundler: ExternalDocsBundler = ExternalDocsBundler()
    private val tagBundler: TagBundler = TagBundler()
    private val operationReplyBundler = OperationReplyBundler()
    private val operationTraitBundler = OperationTraitBundler()
    private val operationReplyAddressBundler = OperationReplyAddressBundler()
    private val bindingBundler = BindingBundler()
    private val serverVariableBundler = ServerVariableBundler()

    fun bundleComponents(
        components: ComponentInterface?,
        visited: Set<String>,
    ): ComponentInterface? {
        if (components == null) return null
        return when (components) {
            is ComponentInterface.ComponentInline ->
                ComponentInterface.ComponentInline(
                    bundleComponent(components.component, visited)
                )

            is ComponentInterface.ComponentReference -> {
                val ref = components.reference.ref
                if (visited.contains(ref)) {
                    components
                } else {
                    val model = components.reference.requireModel<Component>()
                    val newVisited = visited + ref
                    val bundled = bundleComponent(model, newVisited)
                    components.reference.model = bundled
                    components.reference.inline()
                    components
                }
            }
        }
    }

    fun bundleComponent(component: Component, visited: Set<String>): Component {
        val bundledSchemas = schemaBundler.bundleMap(component.schemas, visited)
        val bundledServers = component.servers?.let { serverBundler.bundleServers(it, visited) }
        val bundledChannels = component.channels?.let { channelBundler.bundleMap(it, visited) }
        val bundledOperations = component.operations?.let { operationBundler.bundleMap(it, visited) }
        val bundledMessages = messagesBundler.bundleMap(component.messages, visited)
        val bundledSecuritySchemes = securitySchemeBundler.bundleMap(component.securitySchemes, visited)
        val bundledServerVariables = serverVariableBundler.bundleMap(component.serverVariables, visited)
        val bundledParameters = parameterBundler.bundleMap(component.parameters, visited)
        val bundledCorrelationIds = correlationIdBundler.bundleMap(component.correlationIds, visited)
        val bundledReplies = operationReplyBundler.bundleMap(component.replies, visited)
        val bundledReplyAddresses = operationReplyAddressBundler.bundleMap(component.replyAddresses, visited)
        val bundledExternalDocs = externalDocsBundler.bundleMap(component.externalDocs, visited)
        val bundledTags = tagBundler.bundleMap(component.tags, visited)
        val bundledOperationTraits = operationTraitBundler.bundleMap(component.operationTraits, visited)
        val bundledMessageTraits = messageTraitsBundler.bundleMap(component.messageTraits, visited)
        val bundledServerBindings = bindingBundler.bundleMap(component.serverBindings, visited)
        val bundledChannelBindings = bindingBundler.bundleMap(component.channelBindings, visited)
        val bundledOperationBindings = bindingBundler.bundleMap(component.operationBindings, visited)
        val bundledMessageBindings = bindingBundler.bundleMap(component.messageBindings, visited)
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
}
