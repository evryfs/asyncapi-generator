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
        val classAnnotations: List<String> = emptyList(),
        val classAnnotationImports: List<String> = emptyList(),
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

    data class KafkaConsumerInterface(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topicAddressConstantName: String,
        val topicAddress: String,
        val methods: List<ConsumerMethod>,
        val clientContractAnnotation: String? = null,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem {
        val hasSingleMethod: Boolean get() = methods.size == 1
        val hasMultipleMethods: Boolean get() = methods.size > 1
        val hasPayloadlessMethods: Boolean get() = methods.any { method -> !method.hasPayload }
    }

    data class KafkaProducerClass(
        override val name: String,
        override val packageName: String,
        override val description: List<String>,
        val topicAddressConstantName: String,
        val topicAddress: String,
        val sendMethods: List<SendMethod>,
        val clientContractAnnotation: String? = null,
        val imports: List<String> = emptyList(),
    ) : GeneratorItem {
        val hasSingleMethod: Boolean get() = sendMethods.size == 1
        val hasMultipleMethods: Boolean get() = sendMethods.size > 1
        val hasSinglePayloadMethod: Boolean get() = hasSingleMethod && sendMethods.single().hasPayload
    }

    data class ConsumerMethod(
        val messageName: String,
        val methodName: String,
        val payloadType: String?,
        val payloadDescription: List<String> = emptyList(),
        val keyParameter: KeyParameter? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
        val payloadParameterAnnotation: String? = null,
        val requiredHeaderAnnotation: String? = null,
    ) {
        val hasPayload: Boolean get() = payloadType != null
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasAdditionalParameters: Boolean get() = keyParameter != null || hasHeaders
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
    }

    data class SendMethod(
        val messageName: String,
        val methodName: String,
        val payloadType: String?,
        val payloadDescription: List<String> = emptyList(),
        val payloadBindingAnnotation: String? = null,
        val keyParameter: KeyParameter? = null,
        val headerProperties: List<HeaderProperty> = emptyList(),
        val payloadParameterAnnotation: String? = null,
    ) {
        val hasPayload: Boolean get() = payloadType != null
        val hasHeaders: Boolean get() = headerProperties.isNotEmpty()
        val hasAdditionalParameters: Boolean get() = keyParameter != null || hasHeaders
        val hasParameterDocumentation: Boolean get() = payloadDescription.isNotEmpty() ||
            keyParameter?.description?.isNotEmpty() == true ||
            headerProperties.any { it.description.isNotEmpty() }
        val payloadDescriptionFirstLine: String? get() = payloadDescription.firstOrNull()
        val payloadDescriptionTailLines: List<String> get() = payloadDescription.drop(1)
    }

    data class KeyParameter(
        val parameterName: String,
        val typeName: String,
        val description: List<String> = emptyList(),
        val required: Boolean,
        val annotations: List<String> = emptyList(),
        val parameterSuffix: String = "",
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }

    data class HeaderProperty(
        val wireName: String,
        val parameterName: String,
        val typeName: String,
        val description: List<String> = emptyList(),
        val required: Boolean = false,
        val requiredAnnotation: String? = null,
        val nullableAnnotation: String? = null,
        val parameterSuffix: String = "",
        val bindingAnnotation: String? = null,
    ) {
        val descriptionFirstLine: String? get() = description.firstOrNull()
        val descriptionTailLines: List<String> get() = description.drop(1)
    }
}
