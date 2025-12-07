package com.tietoevry.banking.asyncapi.generator.core.generator.java.model

sealed interface GeneratorItem {
    val name: String
    val packageName: String
    val description: List<String>

    data class ClassModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val properties: List<PropertyModel>,
        val implementsInterfaces: List<String> = emptyList()
    ) : GeneratorItem

    data class EnumModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val values: List<String>
    ) : GeneratorItem

    data class InterfaceModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val discriminator: String? = null,
        val subTypes: List<SubType> = emptyList()
    ) : GeneratorItem {
        data class SubType(
            val name: String,
            val type: String
        )
    }

    data class KafkaHandlerInterface(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val methods: List<HandlerMethod>,
        val imports: List<String> = emptyList()
    ) : GeneratorItem

    data class KafkaListenerClass(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topic: String,
        val groupId: String,
        val handlerInterface: String,
        val messageDispatches: List<MessageDispatch>,
        val imports: List<String> = emptyList()
    ) : GeneratorItem

    data class KafkaProducerClass(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topic: String,
        val sendMethods: List<SendMethod>,
        val imports: List<String> = emptyList()
    ) : GeneratorItem

    data class HandlerMethod(
        val methodName: String,
        val payloadType: String
    )

    data class MessageDispatch(
        val payloadType: String,
        val methodName: String
    )

    data class SendMethod(
        val methodName: String,
        val payloadType: String
    )
}
