package dev.banking.asyncapi.generator.core.parser.messages

import dev.banking.asyncapi.generator.core.model.bindings.Binding
import dev.banking.asyncapi.generator.core.model.bindings.BindingInterface
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationId
import dev.banking.asyncapi.generator.core.model.correlations.CorrelationIdInterface
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.tags.Tag
import dev.banking.asyncapi.generator.core.model.tags.TagInterface
import dev.banking.asyncapi.generator.core.model.messages.Message
import dev.banking.asyncapi.generator.core.model.messages.MessageExample
import dev.banking.asyncapi.generator.core.model.messages.MessageTrait
import dev.banking.asyncapi.generator.core.model.messages.MessageTraitInterface
import dev.banking.asyncapi.generator.core.model.schemas.MultiFormatSchema
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface


fun lightMeasured() = Message(
    name = "lightMeasured",
    title = "Light measured",
    summary = "Inform about environmental lighting conditions of a particular streetlight.",
    contentType = "application/json",

    tags = listOf(
        TagInterface.TagInline(
            Tag(
                name = "telemetry",
                description = "Messages about environmental sensors"
            )
        ),
        TagInterface.TagInline(
            Tag(
                name = "light",
                description = "Messages about light levels"
            )
        )
    ),

    traits = listOf(
        MessageTraitInterface.ReferenceMessageTrait(
            Reference("'#/components/messageTraits/commonHeaders")
        )
    ),

    headers = SchemaInterface.SchemaInline(
        Schema(
            type = "object",
            required = listOf("correlationId"),
            properties = mapOf(
                "correlationId" to SchemaInterface.SchemaInline(
                    Schema(
                        type = "string",
                        description = "Correlation ID set by application"
                    )
                ),
                "applicationInstanceId" to SchemaInterface.SchemaInline(
                    Schema(
                        type = "string",
                        description = "Unique identifier for a given instance of the publishing application"
                    )
                )
            )
        )
    ),

    payload = SchemaInterface.SchemaReference(
        Reference("'#/components/schemas/lightMeasuredPayload")
    ),

    examples = listOf(
        MessageExample(
            name = "lightMeasurementExample",
            summary = "Example of light measurement payload",
            headers = mapOf(
                "my-app-header" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "applicationInstanceId" to mapOf("type" to "string"),
                        "correlationId" to mapOf("type" to "string")
                    )
                )
            ),
            payload = mapOf(
                "lumens" to 1200,
                "sentAt" to "'2024-09-12T12:00:00Z"
            )
        ),
        MessageExample(
            name = "lightMeasurementExample2",
            summary = "Example of light measurement payload 2",
            headers = mapOf(
                "my-app-header" to mapOf(
                    "type" to "object",
                    "properties" to mapOf(
                        "applicationInstanceId" to mapOf("type" to "string"),
                        "correlationId" to mapOf("type" to "string")
                    )
                )
            ),
            payload = mapOf(
                "lumens" to 1200,
                "sentAt" to "'2024-09-12T12:00:00Z"
            )
        )
    )
)

fun turnOnOff() = Message(
    name = "turnOnOff",
    title = "Turn on/off",
    summary = "Command a particular streetlight to turn the lights on or off.",
    bindings = mapOf(
        "amqp" to BindingInterface.BindingInline(
            Binding(
                mapOf(
                    "contentEncoding" to "gzip",
                    "messageType" to "turnOnCommand"
                )
            )
        )
    ),
    examples = listOf(
        MessageExample(
            headers = mapOf(
                "correlationId" to 77
            ),
            payload = mapOf(
                "command" to "'on",
                "sentAt" to "'2024-09-12T12:00:00Z"
            ),
            name = "turnOnExample",
            summary = "Example for turn-on command"
        )
    ),
    traits = listOf(
        MessageTraitInterface.ReferenceMessageTrait(
            Reference("'#/components/messageTraits/commonHeaders")
        )
    )
)

fun referencedMessage() = Reference(
    ref = "'#/components/messages/lightMeasured"
)

fun refPayloadMessage() = Message(
    name = "RefPayload",
    payload = SchemaInterface.SchemaReference(
        Reference("'#/components/schemas/MySchema")
    )
)

fun refCorrelationIdMessage() = Message(
    name = "RefCorrelationId",
    correlationId = CorrelationIdInterface.CorrelationIdReference(
        Reference("'#/components/correlationIds/myId")
    )
)

fun emptyPayloadMessage() = Message(
    name = "empty",
    payload = null
)

fun inlineTraitMessage() = Message(
    name = "InlineTraitMessage",
    traits = listOf(
        MessageTraitInterface.InlineMessageTrait(
            MessageTrait(
                headers = SchemaInterface.SchemaInline(
                    Schema(type = "string")
                )
            )
        )
    )
)
