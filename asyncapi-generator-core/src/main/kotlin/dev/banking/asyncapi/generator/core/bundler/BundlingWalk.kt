package dev.banking.asyncapi.generator.core.bundler

import dev.banking.asyncapi.generator.core.bundler.bindings.BindingBundler
import dev.banking.asyncapi.generator.core.bundler.channels.ChannelBundler
import dev.banking.asyncapi.generator.core.bundler.components.ComponentBundler
import dev.banking.asyncapi.generator.core.bundler.info.InfoBundler
import dev.banking.asyncapi.generator.core.bundler.messages.MessageBundler
import dev.banking.asyncapi.generator.core.bundler.messages.MessageTraitBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationReplyBundler
import dev.banking.asyncapi.generator.core.bundler.operations.OperationTraitBundler
import dev.banking.asyncapi.generator.core.bundler.schemas.SchemaBundler
import dev.banking.asyncapi.generator.core.bundler.servers.ServerBundler
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.channels.ChannelInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageInterface
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.parameters.ParameterInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.ReferenceOr
import dev.banking.asyncapi.generator.core.model.security.SecuritySchemeInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerInterface
import dev.banking.asyncapi.generator.core.model.servers.ServerVariableInterface
import dev.banking.asyncapi.generator.core.model.tags.TagInterface

/**
 * Walks the model for the bundler: holds one bundler per object kind and does the step every member
 * shares, bundling an inline model or a reference to one.
 *
 * Object bundlers decide which members of their object they rebuild; the walk decides what happens to
 * each member. The walk keeps no state of its own: what one bundling shares lives in its [BundlingRun],
 * reached through the [BundlingScope].
 */
internal class BundlingWalk {
    /** Bundles schemas, which are not inline-or-reference members: their references can also be kept or promoted. */
    val schemas = SchemaBundler(this)
    val info = InfoBundler(this)
    val components = ComponentBundler(this)
    private val servers = ServerBundler(this)
    private val channels = ChannelBundler(this)
    private val operations = OperationBundler(this)
    private val operationTraits = OperationTraitBundler(this)
    private val replies = OperationReplyBundler(this)
    private val messages = MessageBundler(this)
    private val messageTraits = MessageTraitBundler(this)
    private val bindings = BindingBundler(this)

    val server = MemberKind(ServerInterface::ServerInline, ServerInterface::ServerReference, servers::bundle)
    val channel = MemberKind(ChannelInterface::ChannelInline, ChannelInterface::ChannelReference, channels::bundle)
    val operation =
        MemberKind(OperationInterface::OperationInline, OperationInterface::OperationReference, operations::bundle)
    val operationTrait =
        MemberKind(
            OperationTraitInterface::OperationTraitInline,
            OperationTraitInterface::OperationTraitReference,
            operationTraits::bundle,
        )
    val reply =
        MemberKind(
            OperationReplyInterface::OperationReplyInline,
            OperationReplyInterface::OperationReplyReference,
            replies::bundle,
        )
    val message = MemberKind(MessageInterface::MessageInline, MessageInterface::MessageReference, messages::bundle)
    val messageTrait =
        MemberKind(
            MessageTraitInterface::MessageTraitInline,
            MessageTraitInterface::MessageTraitReference,
            messageTraits::bundle,
        )
    val binding = MemberKind(BindingInterface::BindingInline, BindingInterface::BindingReference, bindings::bundle)
    val tag = MemberKind(TagInterface::TagInline, TagInterface::TagReference)
    val externalDoc = MemberKind(ExternalDocInterface::ExternalDocInline, ExternalDocInterface::ExternalDocReference)
    val parameter = MemberKind(ParameterInterface::ParameterInline, ParameterInterface::ParameterReference)
    val correlationId =
        MemberKind(CorrelationIdInterface::CorrelationIdInline, CorrelationIdInterface::CorrelationIdReference)
    val securityScheme =
        MemberKind(SecuritySchemeInterface::SecuritySchemeInline, SecuritySchemeInterface::SecuritySchemeReference)
    val serverVariable =
        MemberKind(ServerVariableInterface::ServerVariableInline, ServerVariableInterface::ServerVariableReference)
    val replyAddress =
        MemberKind(
            OperationReplyAddressInterface::OperationReplyAddressInline,
            OperationReplyAddressInterface::OperationReplyAddressReference,
        )

    /**
     * Bundles one member: an inline model has its members bundled; a reference not yet followed on this path
     * becomes an inline copy of its bundled model; a reference already followed on this path stays a `$ref`,
     * which ends recursion.
     */
    fun <I : ReferenceOr<T>, T : Any> member(
        node: I?,
        kind: MemberKind<I, T>,
        scope: BundlingScope,
    ): I? = node?.let { bundleMember(it, kind, scope) }

    /** Bundles each member of a list, keeping its order. */
    fun <I : ReferenceOr<T>, T : Any> members(
        nodes: List<I>?,
        kind: MemberKind<I, T>,
        scope: BundlingScope,
    ): List<I>? = nodes?.map { bundleMember(it, kind, scope) }

    /** Bundles each member of a map, keeping its keys and order. */
    fun <I : ReferenceOr<T>, T : Any> members(
        nodes: Map<String, I>?,
        kind: MemberKind<I, T>,
        scope: BundlingScope,
    ): Map<String, I>? = nodes?.mapValues { (_, node) -> bundleMember(node, kind, scope) }

    private fun <I : ReferenceOr<T>, T : Any> bundleMember(
        node: I,
        kind: MemberKind<I, T>,
        scope: BundlingScope,
    ): I {
        val bundle = kind.bundle
        val reference = node.held() as? Reference
        return when {
            reference == null -> if (bundle == null) node else kind.inline(bundle(node.linkedModel(), scope))
            scope.hasVisited(reference) -> node
            bundle == null -> kind.reference(reference.inlined(node.linkedModel()))
            else -> kind.reference(reference.inlined(bundle(node.linkedModel(), scope.enter(reference))))
        }
    }

    private fun <T : Any> ReferenceOr<T>.linkedModel(): T =
        checkNotNull(resolvedOrNull()) {
            "Reference '${(held() as Reference).ref}' is not linked to a model"
        }
}
