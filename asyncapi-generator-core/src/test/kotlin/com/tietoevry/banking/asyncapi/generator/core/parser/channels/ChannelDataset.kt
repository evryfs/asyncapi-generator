package com.tietoevry.banking.asyncapi.generator.core.parser.channels

import com.tietoevry.banking.asyncapi.generator.core.model.channels.Channel
import com.tietoevry.banking.asyncapi.generator.core.model.messages.MessageInterface
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.Binding
import com.tietoevry.banking.asyncapi.generator.core.model.bindings.BindingInterface
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.model.parameters.Parameter
import com.tietoevry.banking.asyncapi.generator.core.model.parameters.ParameterInterface

fun lightingMeasured() = Channel(
    address = "'smartylighting.streetlights.1.0.event.{streetlightId}.lighting.measured",
    description = "The topic on which measured values may be produced and consumed.",
    messages = mapOf(
        "lightMeasured" to MessageInterface.MessageReference(
            Reference("'#/components/messages/lightMeasured")
        )
    ),
    parameters = mapOf(
        "streetlightId" to ParameterInterface.ParameterReference(
            Reference("'#/components/parameters/streetlightId")
        )
    ),
    bindings = mapOf(
        "kafka" to BindingInterface.BindingInline(
            Binding(
                content = mapOf(
                    "topic" to "smartylighting.streetlights.1.0.event",
                    "partitions" to 3,
                    "replicas" to 1
                )
            )
        )
    )
)

fun lightTurnOn() = Channel(
    address = "'smartylighting.streetlights.1.0.action.{streetlightId}.turn.on",
    messages = mapOf(
        "turnOn" to MessageInterface.MessageReference(
            Reference("'#/components/messages/turnOnOff")
        )
    ),
    parameters = mapOf(
        "streetlightId" to ParameterInterface.ParameterReference(
            Reference("'#/components/parameters/streetlightId")
        )
    )
)

fun lightTurnOff() = Channel(
    address = "'smartylighting.streetlights.1.0.action.{streetlightId}.turn.off",
    messages = mapOf(
        "turnOff" to MessageInterface.MessageReference(
            Reference("'#/components/messages/turnOnOff")
        )
    ),
    parameters = mapOf(
        "streetlightId" to ParameterInterface.ParameterReference(
            Reference("'#/components/parameters/streetlightId")
        )
    )
)

fun lightsDim() = Channel(
    address = "'smartylighting.streetlights.1.0.action.{streetlightId}.dim",
    messages = mapOf(
        "dimLight" to MessageInterface.MessageReference(
            Reference("'#/components/messages/dimLight")
        )
    ),
    parameters = mapOf(
        "streetlightId" to ParameterInterface.ParameterReference(
            Reference("'#/components/parameters/streetlightId")
        )
    )
)

fun lightStatus() = Channel(
    address = "'smartylighting.streetlights.1.0.event.{city}.status",
    description = "The topic reporting light status by city.",
    messages = mapOf(
        "lightStatusMessage" to MessageInterface.MessageReference(
            Reference("'#/components/messages/lightMeasured")
        )
    ),
    parameters = mapOf(
        "city" to ParameterInterface.ParameterInline(
            Parameter(
                description = "The city where the streetlights are located.",
                location = "\"\$message.payload#/city",
                enum = listOf("helsinki", "oslo", "stockholm"),
                default = "helsinki",
                examples = listOf("helsinki", "oslo")
            )
        )
    )
)

fun maintenanceRequest() = Channel(
    address = "'smartylighting.streetlights.1.0.action.{requestId}.maintenance",
    description = "Command topic for maintenance requests.",
    messages = mapOf(
        "maintenanceMessage" to MessageInterface.MessageReference(
            Reference("'#/components/messages/turnOnOff")
        )
    ),
    parameters = mapOf(
        "requestId" to ParameterInterface.ParameterInline(
            Parameter(
                description = "Identifier for maintenance request.",
                default = "\"req-001",
                location = $$"\"$message.header#/requestId"
            )
        )
    )
)

fun cityLights() = Channel(
    address = "'smartylighting.streetlights.1.0.{cityId}.light.{lightId}",
    description = "Channel for controlling individual lights in a city.",
    messages = mapOf(
        "dimLight" to MessageInterface.MessageReference(
            Reference("'#/components/messages/dimLight")
        )
    ),
    parameters = mapOf(
        "cityId" to ParameterInterface.ParameterReference(
            Reference("'#/components/parameters/cityId")
        ),
        "lightId" to ParameterInterface.ParameterInline(
            Parameter(
                description = "Identifier of the specific light.",
                enum = listOf("lamp-001", "lamp-002", "lamp-003"),
                examples = listOf("lamp-001", "lamp-002"),
                location = $$"\"$message.header#/lightId"
            )
        )
    )
)

fun powerStatus() = Channel(
    address = "'smartylighting.streetlights.1.0.power.{streetlightId}.status",
    description = "Channel for power status updates.",
    messages = mapOf(
        "powerMessage" to MessageInterface.MessageReference(
            Reference("'#/components/messages/lightMeasured")
        )
    ),
    parameters = mapOf(
        "streetlightId" to ParameterInterface.ParameterInline(
            Parameter(
                description = "Identifier for the streetlight.",
                location = "\"\$message.header#/streetlightId"
            )
        )
    )
)
