package dev.banking.asyncapi.generator.core.generator.configuration

/**
 * Resolves model defaults and validates model compatibility for a generator.
 */
object ModelTypeResolver {
    /**
     * The configured model type, or the language's default, checked against the
     * model types the source generator [generatorName] supports.
     */
    fun resolve(
        generatorName: GeneratorName,
        configuredModelType: ModelType?,
    ): ModelType {
        val language =
            requireNotNull(generatorName.sourceLanguage) {
                "generatorName '${generatorName.configurationValue}' does not generate source code"
            }
        val modelType = configuredModelType ?: language.defaultModelType
        require(modelType in language.supportedModelTypes) {
            "modelConfig.modelType '${modelType.configurationValue}' is not supported when " +
                "generatorName is ${generatorName.configurationValue}. Supported values: " +
                language.supportedModelTypes.joinToString(", ") { it.configurationValue }
        }
        return modelType
    }
}
