package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.bundler.AsyncApiDocumentSerializer
import dev.banking.asyncapi.generator.core.generator.configuration.DocumentFormat
import dev.banking.asyncapi.generator.core.generator.output.GeneratedDocumentArtifact
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Serializes a bundled AsyncAPI document in the configured output format.
 */
internal class DocumentArtifactGeneration {
    fun render(
        task: GenerationTask.DocumentArtifact,
        asyncApiDocument: AsyncApiDocument,
    ): GenerationResult =
        GenerationResult.of(
            GeneratedDocumentArtifact(
                file = task.document.file,
                content =
                    when (task.document.format) {
                        DocumentFormat.YAML -> AsyncApiDocumentSerializer.toYaml(asyncApiDocument)
                        DocumentFormat.JSON -> AsyncApiDocumentSerializer.toJson(asyncApiDocument)
                    },
            ),
        )
}
