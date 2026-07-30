package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry

/**
 * Serializes a bundled AsyncAPI document in the configured output format.
 *
 * Expected behavior is covered by:
 * - `AsyncApiGeneratorOutputContractTest`
 */
class DocumentArtifactGeneration {
    fun generate(
        task: GenerationTask.DocumentArtifact,
        asyncApiDocument: AsyncApiDocument,
    ) {
        when (task.format) {
            DocumentFormat.YAML -> AsyncApiRegistry.writeYaml(task.file, asyncApiDocument)
            DocumentFormat.JSON -> AsyncApiRegistry.writeJson(task.file, asyncApiDocument)
        }
    }
}
