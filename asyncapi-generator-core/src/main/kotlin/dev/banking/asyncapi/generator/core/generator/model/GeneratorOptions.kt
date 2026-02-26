package dev.banking.asyncapi.generator.core.generator.model

import java.io.File

data class GeneratorOptions(
    val generatorName: GeneratorName,
    val modelPackage: String,
    val clientPackage: String,
    val schemaPackage: String,
    val codegenOutputDirectory: File,
    val resourceOutputDirectory: File,

    // Feature Flags
    val generateModels: Boolean = true,
    val generateSpringKafkaClient: Boolean = false,
    val generateQuarkusKafkaClient: Boolean = false,
    val generateAvroSchema: Boolean = false,
    // Flat config options (for future use)
    val configOptions: Map<String, String> = emptyMap()
)
