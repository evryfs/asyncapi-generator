package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.output.GeneratedDocumentArtifact
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry

/**
 * Serializes a bundled AsyncAPI document in the configured output format.
 */
class DocumentArtifactGeneration {
    fun render(
        task: GenerationTask.DocumentArtifact,
        asyncApiDocument: AsyncApiDocument,
    ): GenerationResult =
        GenerationResult.of(
            GeneratedDocumentArtifact(
                file = task.file,
                content =
                    when (task.format) {
                        DocumentFormat.YAML -> AsyncApiRegistry.serializeYaml(asyncApiDocument)
                        DocumentFormat.JSON -> AsyncApiRegistry.serializeJson(asyncApiDocument)
                    },
            ),
        )
}
