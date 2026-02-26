package dev.banking.asyncapi.generator.core.generator.kotlin.model

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
        val classAnnotations: List<String> = emptyList(),
        val classAnnotationImports: List<String> = emptyList(),
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
        val kafkaValueType: String,
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
