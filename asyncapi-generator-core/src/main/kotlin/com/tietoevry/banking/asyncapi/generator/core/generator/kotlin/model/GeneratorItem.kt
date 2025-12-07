package com.tietoevry.banking.asyncapi.generator.core.generator.kotlin.model

/**
 * Represents the "Rich Model" contract between the schema processing "frontend" and
 * the code generation "backend".
 *
 * This sealed interface and its implementations are the final, unambiguous output of all
 * schema analysis and processing. All complex logic (composition, discovery, type mapping,
 * constraint building, etc.) is performed *before* these models are created.
 *
 * The code generator "backend" then becomes a simple, "dumb" system that just takes these
 * rich models and uses them to populate templates, with no further logic or decision-making required.
 */
sealed interface GeneratorItem {
    val name: String
    val packageName: String
    val description: List<String>

    data class DataClassModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val properties: List<PropertyModel>,
        val parentInterfaces: List<String>,
    ) : GeneratorItem

    data class EnumClassModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val values: List<String>,
    ) : GeneratorItem

    data class SealedInterfaceModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
    ) : GeneratorItem

    data class KafkaHandlerInterface(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val methods: List<HandlerMethod>,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem

    data class KafkaListenerClass(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topic: String,
        val groupId: String,
        val handlerInterface: String, // The interface to inject
        val messageDispatches: List<MessageDispatch>,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem

    data class KafkaProducerClass(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topic: String,
        val sendMethods: List<SendMethod>,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem

    data class HandlerMethod(
        val methodName: String,
        val payloadType: String,
        val keyType: String?,
    )

    data class MessageDispatch(
        val payloadType: String,
        val methodName: String,
    )

    data class SendMethod(
        val methodName: String,
        val payloadType: String,
        val keyType: String?,
    )
}
