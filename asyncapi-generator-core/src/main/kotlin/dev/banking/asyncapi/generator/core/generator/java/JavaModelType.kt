package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.ModelType

/**
 * Java source shape of generated Java models.
 */
internal enum class JavaModelType {
    CLASS,
    RECORD,
    ;

    companion object {
        /** The source shape of a Java [modelType]. */
        fun of(modelType: ModelType): JavaModelType =
            when (modelType) {
                ModelType.JAVA_CLASS -> CLASS
                ModelType.JAVA_RECORD -> RECORD
                ModelType.KOTLIN_DATA_CLASS,
                ModelType.AVRO_SPECIFIC_RECORD,
                ModelType.PROTOBUF_MESSAGE,
                -> throw IllegalArgumentException(
                    "Model type '${modelType.configurationValue}' is not a Java model type",
                )
            }
    }
}
