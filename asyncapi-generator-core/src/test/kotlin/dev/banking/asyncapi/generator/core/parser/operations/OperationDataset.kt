package dev.banking.asyncapi.generator.core.parser.operations

import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDoc
import dev.banking.asyncapi.generator.core.model.externaldocs.ExternalDocInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.operations.Operation
import dev.banking.asyncapi.generator.core.model.operations.OperationReply
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddress
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyAddressInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationReplyInterface
import dev.banking.asyncapi.generator.core.model.operations.OperationTraitInterface
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.OPERATION_TRAIT
import dev.banking.asyncapi.generator.core.model.references.ReferenceCategoryKey.REFERENCE

fun receiveLightMeasurement() = Operation(
    action = "receive",
    summary = "Inform about environmental lighting conditions of a particular streetlight.",
    channel = Reference(ref = "'#/channels/lightingMeasured", referenceCategoryKey = REFERENCE),
    messages = listOf(
        Reference(ref = "'#/components/messages/lightMeasured", referenceCategoryKey = REFERENCE)
    ),
    traits = listOf(
        OperationTraitInterface.OperationTraitReference(
            reference = Reference(
                ref = "'#/components/operationTraits/kafka",
                referenceCategoryKey = OPERATION_TRAIT
            )
        )
    ),
    externalDocs = ExternalDocInterface.ExternalDocInline(
        externalDoc = ExternalDoc(
            url = "'https://example.com/api/oauth/dialog"
        )
    ),
    reply = OperationReplyInterface.OperationReplyInline(
        operationReply = OperationReply(
            address = OperationReplyAddressInterface.OperationReplyAddressInline(
                operationReplyAddress = OperationReplyAddress(
                    location = $$"'$message.header#/replyTo"
                )
            ),
            channel = Reference(ref = "'#/channels/lightingMeasured", referenceCategoryKey = REFERENCE),
            messages = listOf(
                Reference(ref = "'#/components/messages/lightMeasured", referenceCategoryKey = REFERENCE)
            )
        )
    )
)

fun turnOn() = Operation(
    action = "send",
    channel = Reference(ref = "'#/channels/lightTurnOn", referenceCategoryKey = REFERENCE),
    messages = listOf(
        Reference(ref = "'#/components/messages/turnOn", referenceCategoryKey = REFERENCE)
    ),
    bindings = mapOf(
        "amqp" to BindingInterface.BindingInline(
            binding = Binding(content = mapOf("ack" to false))
        )
    ),
    traits = listOf(
        OperationTraitInterface.OperationTraitReference(
            reference = Reference(
                ref = "'#/components/operationTraits/kafka",
                referenceCategoryKey = OPERATION_TRAIT
            )
        )
    ),
    tags = listOf(
        TagInterface.TagInline(tag = Tag(name = "user")),
        TagInterface.TagInline(tag = Tag(name = "signup")),
        TagInterface.TagInline(
            tag = Tag(
                name = "register",
                externalDocs = ExternalDocInterface.ExternalDocInline(
                    externalDoc = ExternalDoc(
                        url = "\"https://example.com/docs/register",
                        description = "Details about registration flows"
                    )
                )
            )
        )
    ),
    externalDocs = ExternalDocInterface.ExternalDocInline(
        externalDoc = ExternalDoc(
            url = "'https://example.com/api/oauth/dialog"
        )
    ),
    reply = OperationReplyInterface.OperationReplyInline(
        operationReply = OperationReply(
            address = OperationReplyAddressInterface.OperationReplyAddressInline(
                operationReplyAddress = OperationReplyAddress(
                    location = $$"\"$message.header#/replyTo"
                )
            ),
            channel = Reference(ref = "\"#/channels/lightTurnOn", referenceCategoryKey = REFERENCE),
            messages = listOf(
                Reference(ref = "\"#/components/messages/turnOn", referenceCategoryKey = REFERENCE)
            )
        )
    )
)
