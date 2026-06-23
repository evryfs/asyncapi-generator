package dev.banking.asyncapi.generator.core.generator.java.model

sealed interface GeneratorItem {
    val name: String
    val packageName: String
    val description: List<String>

    data class ClassModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val properties: List<PropertyModel>,
        val implementsInterfaces: List<String> = emptyList(),
    ) : GeneratorItem

    data class EnumModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val values: List<String>,
    ) : GeneratorItem

    data class InterfaceModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val discriminator: String? = null,
        val subTypes: List<SubType> = emptyList(),
    ) : GeneratorItem {
        data class SubType(
            val name: String,
            val type: String,
        )
    }

    data class KafkaHandlerInterface(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val methods: List<HandlerMethod>,
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
        val payloadDescription: List<String> = emptyList(),
        val keyDescription: List<String> = emptyList(),
        val headerType: String? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
    ) {
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasParameterDocumentation: Boolean get() = payloadDescription.isNotEmpty() ||
            keyDescription.isNotEmpty() ||
            headerProperties.any { it.description.isNotEmpty() }
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
        val keyDescriptionFirstLine: String? get() = keyDescription.firstOrNull()
        val keyDescriptionTailLines: List<String> get() = keyDescription.drop(1)
    }

    data class SendMethod(
        val methodName: String,
        val payloadType: String,
        val payloadDescription: List<String> = emptyList(),
        val keyDescription: List<String> = emptyList(),
        val headerType: String? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
    ) {
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasParameterDocumentation: Boolean get() = payloadDescription.isNotEmpty() ||
            keyDescription.isNotEmpty() ||
            headerProperties.any { it.description.isNotEmpty() }
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
        val keyDescriptionFirstLine: String? get() = keyDescription.firstOrNull()
        val keyDescriptionTailLines: List<String> get() = keyDescription.drop(1)
    }

    data class HeaderProperty(
        val name: String,
        val accessorName: String,
        val parameterName: String,
        val typeName: String,
        val description: List<String> = emptyList(),
        val required: Boolean = false,
        val nullableAnnotation: String? = null,
        val parameterSuffix: String = "",
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }
}
