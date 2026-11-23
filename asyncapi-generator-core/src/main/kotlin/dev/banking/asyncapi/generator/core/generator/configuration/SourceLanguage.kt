package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Programming language of the source code a source generator writes.
 */
enum class SourceLanguage {
    KOTLIN,
    JAVA,
    ;

    /** Model type used when the configuration names none. */
    val defaultModelType: ModelType
        get() =
            when (this) {
                KOTLIN -> ModelType.KOTLIN_DATA_CLASS
                JAVA -> ModelType.JAVA_CLASS
            }

    /** Model types this language can generate, in declaration order. */
    val supportedModelTypes: List<ModelType>
        get() = ModelType.entries.filter { it.language == null || it.language == this }
}
