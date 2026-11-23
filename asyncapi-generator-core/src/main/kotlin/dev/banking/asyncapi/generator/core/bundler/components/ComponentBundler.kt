package dev.banking.asyncapi.generator.core.bundler.components

import dev.banking.asyncapi.generator.core.bundler.BundlingScope
import dev.banking.asyncapi.generator.core.bundler.BundlingWalk
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface

/**
 * Bundles the reusable objects of `components`.
 *
 * @param walk bundles each reusable object
 */
internal class ComponentBundler(
    private val walk: BundlingWalk,
) {
    /** Returns [component] with every reusable object bundled. */
    fun bundle(
        component: Component,
        scope: BundlingScope,
    ): Component =
        component.copy(
            schemas = walk.schemas.bundleComponentMap(component.schemas, scope),
            servers = walk.members(component.servers, walk.server, scope),
            channels = walk.members(component.channels, walk.channel, scope),
            operations = walk.members(component.operations, walk.operation, scope),
            messages = walk.members(component.messages, walk.message, scope),
            securitySchemes = walk.members(component.securitySchemes, walk.securityScheme, scope),
            serverVariables = walk.members(component.serverVariables, walk.serverVariable, scope),
            parameters = walk.members(component.parameters, walk.parameter, scope),
            correlationIds = walk.members(component.correlationIds, walk.correlationId, scope),
            replies = walk.members(component.replies, walk.reply, scope),
            replyAddresses = walk.members(component.replyAddresses, walk.replyAddress, scope),
            externalDocs = walk.members(component.externalDocs, walk.externalDoc, scope),
            tags = walk.members(component.tags, walk.tag, scope),
            operationTraits = walk.members(component.operationTraits, walk.operationTrait, scope),
            messageTraits = walk.members(component.messageTraits, walk.messageTrait, scope),
            serverBindings = walk.members(component.serverBindings, walk.binding, scope),
            channelBindings = walk.members(component.channelBindings, walk.binding, scope),
            operationBindings = walk.members(component.operationBindings, walk.binding, scope),
            messageBindings = walk.members(component.messageBindings, walk.binding, scope),
        )

    /** Adds the promoted [schemas] to the bundled [components]. */
    fun withPromotedSchemas(
        components: Component?,
        schemas: Map<String, SchemaInterface>,
    ): Component? {
        if (schemas.isEmpty()) return components
        return components?.copy(schemas = components.schemas.orEmpty() + schemas) ?: Component(schemas = schemas)
    }
}
