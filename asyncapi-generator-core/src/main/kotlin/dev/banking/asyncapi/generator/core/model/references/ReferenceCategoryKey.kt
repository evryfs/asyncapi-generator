package dev.banking.asyncapi.generator.core.model.references

import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.channels.Channel
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationTrait
import dev.banking.asyncapi.generator.core.model.parameters.Parameter
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.model.security.SecurityScheme
import dev.banking.asyncapi.generator.core.model.servers.Server
import dev.banking.asyncapi.generator.core.model.servers.ServerVariable
import dev.banking.asyncapi.generator.core.model.tags.Tag
import kotlin.reflect.KClass

/**
 * The kind of object a `$ref` must point at.
 *
 * @param targetTypes the model types an object of this kind is parsed into
 */
enum class ReferenceCategoryKey(
    private vararg val targetTypes: KClass<*>,
) {
    SCHEMA(Schema::class, MultiFormatSchema::class, SchemaInterface.BooleanSchema::class),
    CHANNEL(Channel::class),
    MESSAGE(Message::class),
    MESSAGE_TRAIT(MessageTrait::class),
    OPERATION(Operation::class),
    OPERATION_TRAIT(OperationTrait::class),
    OPERATION_REPLY(OperationReply::class),
    OPERATION_REPLY_ADDRESS(OperationReplyAddress::class),
    SERVER(Server::class),
    SERVER_VARIABLE(ServerVariable::class),
    PARAMETER(Parameter::class),
    SECURITY_SCHEME(SecurityScheme::class),
    CORRELATION_ID(CorrelationId::class),
    EXTERNAL_DOC(ExternalDoc::class),
    TAG(Tag::class),
    BINDING(Binding::class),
    ;

    /** The name of this kind in diagnostics, such as `message trait object`. */
    val displayName: String
        get() = name.lowercase().replace('_', ' ') + " object"

    /** Whether [target] is a model of this kind. */
    fun accepts(target: Any): Boolean = targetTypes.any { type -> type.isInstance(target) }
}
