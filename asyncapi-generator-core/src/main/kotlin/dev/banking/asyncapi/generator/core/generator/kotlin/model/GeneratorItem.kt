package dev.banking.asyncapi.generator.core.generator.kotlin.model

sealed interface GeneratorItem {
    val name: String
    val packageName: String
    val description: List<String>

    data class TypeAliasModel(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val aliasType: String,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem

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
        val keyType: String?,
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
        val keyType: String?,
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
        val defaultValue: String? = null,
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }
}
