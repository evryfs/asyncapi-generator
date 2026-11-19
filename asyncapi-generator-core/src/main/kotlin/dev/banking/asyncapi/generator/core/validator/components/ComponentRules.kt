package dev.banking.asyncapi.generator.core.validator.components

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.BINDING
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CHANNEL
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.CORRELATION_ID
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.EXTERNAL_DOC
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.MESSAGE_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_REPLY_ADDRESS
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.PARAMETER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SECURITY_SCHEME
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.SERVER_VARIABLE
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.TAG
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.COMPONENT_NAME_FORMAT
import dev.banking.asyncapi.generator.core.validator.util.ValidationFormats
import dev.banking.asyncapi.generator.core.validator.walk.ObjectRules
import dev.banking.asyncapi.generator.core.validator.walk.ValidationPath
import dev.banking.asyncapi.generator.core.validator.walk.ValidationWalk
import kotlin.reflect.KProperty0

/**
 * Validates the Components Object: the names of its reusable objects, then walks every reusable object.
 *
 * @param asyncApiContext the load the components belong to
 */
internal class ComponentRules(
    private val asyncApiContext: AsyncApiContext,
) : ObjectRules<Component> {
    override fun check(
        node: Component,
        path: ValidationPath,
        walk: ValidationWalk,
    ) {
        if (!walk.results.visit(node)) return

        validateNames(node, walk)
        node.schemas?.let { walk.schemaRules.checkMap(it, path, walk) }
        members(node).forEach { members ->
            members.objects?.forEach { (name, member) ->
                walk.member(member, members.category, path.named(members.kind, name))
            }
        }
    }

    private fun validateNames(
        component: Component,
        walk: ValidationWalk,
    ) {
        reusableObjectMaps(component).forEach { property ->
            property.get()?.keys?.filterNot(ValidationFormats::isComponentName)?.forEach { name ->
                walk.results.error(
                    COMPONENT_NAME_FORMAT,
                    "Component ${property.name} key '$name' must match ^[a-zA-Z0-9.\\-_]+$.",
                    sourceLocation = asyncApiContext.modelTracking.getSourceLocation(component, property.name),
                )
            }
        }
    }

    private fun reusableObjectMaps(component: Component): List<KProperty0<Map<String, *>?>> =
        listOf(
            component::schemas,
            component::servers,
            component::channels,
            component::operations,
            component::messages,
            component::securitySchemes,
            component::serverVariables,
            component::parameters,
            component::correlationIds,
            component::replies,
            component::replyAddresses,
            component::externalDocs,
            component::tags,
            component::operationTraits,
            component::messageTraits,
            component::serverBindings,
            component::channelBindings,
            component::operationBindings,
            component::messageBindings,
        )

    /**
     * The reusable objects of one kind, in the order the components are walked.
     *
     * @param kind the kind as findings name it
     * @param category the kind of object the members hold
     * @param objects the members by name
     */
    private class Members(
        val kind: String,
        val category: ReferenceCategoryKey,
        val objects: Map<String, ReferenceOr<*>>?,
    )

    private fun members(component: Component): List<Members> =
        listOf(
            Members("Server", SERVER, component.servers),
            Members("Channel", CHANNEL, component.channels),
            Members("Operation", OPERATION, component.operations),
            Members("Message", MESSAGE, component.messages),
            Members("Security Scheme", SECURITY_SCHEME, component.securitySchemes),
            Members("Server Variable", SERVER_VARIABLE, component.serverVariables),
            Members("Parameter", PARAMETER, component.parameters),
            Members("Correlation ID", CORRELATION_ID, component.correlationIds),
            Members("Operation Reply", OPERATION_REPLY, component.replies),
            Members("Operation Reply Address", OPERATION_REPLY_ADDRESS, component.replyAddresses),
            Members("External Doc", EXTERNAL_DOC, component.externalDocs),
            Members("Tag", TAG, component.tags),
            Members("Operation Trait", OPERATION_TRAIT, component.operationTraits),
            Members("Message Trait", MESSAGE_TRAIT, component.messageTraits),
            Members("Server Binding", BINDING, component.serverBindings),
            Members("Channel Binding", BINDING, component.channelBindings),
            Members("Operation Binding", BINDING, component.operationBindings),
            Members("Message Binding", BINDING, component.messageBindings),
        )
}
