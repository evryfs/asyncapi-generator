package dev.banking.asyncapi.generator.gradle.plugin

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationFactory
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfigurationRequest
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

/**
 * Gradle-facing values used to assemble the frontend-neutral generator configuration.
 */
internal data class GradleGeneratorConfigurationRequest(
    val generatorName: String?,
    val outputDirectory: File,
    val outputFile: File? = null,
    val modelPackage: String? = null,
    val clientPackage: String? = null,
    val schemaPackage: String? = null,
    val modelConfig: GradleModelConfiguration? = null,
    val clientConfig: GradleClientConfiguration? = null,
)

/**
 * Maps and validates Gradle plugin configuration before contract processing starts.
 *
 * Expected behavior is covered by:
 * - `GradleGeneratorConfigurationMapperTest`
 */
internal object GradleGeneratorConfigurationMapper {
    fun map(request: GradleGeneratorConfigurationRequest): GeneratorConfiguration {
        validateGradleConfiguration(request)

        val targetGenerator =
            GeneratorName.fromConfigurationValue(
                value = request.generatorName,
                path = "generatorName",
            )
        val modelRequest =
            if (request.modelPackage != null || request.modelConfig != null) {
                (request.modelConfig ?: GradleModelConfiguration()).toRequest(request.modelPackage)
            } else {
                null
            }
        val configuration =
            GeneratorConfigurationFactory.create(
                GeneratorConfigurationRequest(
                    generatorName = targetGenerator,
                    sourceOutputDirectory = request.outputDirectory,
                    javaSourceOutputDirectory = request.outputDirectory,
                    resourceOutputDirectory = request.outputDirectory,
                    outputFile = request.outputFile,
                    schemaPackageName = request.schemaPackage,
                    models = modelRequest,
                    clients =
                        request.clientConfig?.toRequest(
                            clientPackage = request.clientPackage,
                            modelPackage = request.modelPackage,
                        ) ?: GeneratorConfigurationRequest.Clients(),
                ),
            )

        validateActivatedOutputs(request, configuration)
        return configuration
    }

    private fun validateGradleConfiguration(request: GradleGeneratorConfigurationRequest) {
        if (request.clientPackage != null && request.clientConfig == null) {
            throw IllegalArgumentException("clientConfig is required when clientPackage is configured")
        }
        if (request.outputDirectory.exists() && !request.outputDirectory.isDirectory) {
            throw IllegalArgumentException(
                "outputDirectory must be a directory: ${request.outputDirectory}",
            )
        }
        if (request.outputFile?.isDirectory == true) {
            throw IllegalArgumentException("outputFile must be a file: ${request.outputFile}")
        }
    }

    private fun validateActivatedOutputs(
        request: GradleGeneratorConfigurationRequest,
        configuration: GeneratorConfiguration,
    ) {
        if (request.schemaPackage != null && configuration.schemas.isEmpty()) {
            throw IllegalArgumentException(
                "schemaPackage is only supported by schema generator profiles and native Avro or Protobuf models",
            )
        }
        if (!configuration.hasConfiguredOutputs()) {
            throw IllegalArgumentException(
                "No generator output is configured. Configure modelPackage, clientPackage with clientConfig, " +
                    "schemaPackage with a schema generator, or outputFile.",
            )
        }
    }
}
