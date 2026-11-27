package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.bundler.BundledAsyncApiDocument
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.input.GenerationInputFactory
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.protobuf.MavenRepositoryProtocProvider
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocProvider
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.NoArtifactsGenerated

/**
 * Coordinates generator input preparation, planning, rendering, and artifact writing.
 *
 * @param protocProvider Supplies `protoc` for native Protobuf model generation; frontends pass one
 *   that resolves it through their build tool's repositories.
 */
class AsyncApiGenerator(
    protocProvider: ProtocProvider = MavenRepositoryProtocProvider(),
) {
    private val generationInputFactory = GenerationInputFactory()
    private val generationPlanner = GenerationPlanner()
    private val generationTasks = GenerationTasks(protocProvider)

    fun generate(
        bundled: BundledAsyncApiDocument,
        generatorConfiguration: GeneratorConfiguration,
    ) {
        val document = bundled.document
        val context =
            GenerationContext(
                document = document,
                plan = generationPlanner.plan(generatorConfiguration),
                prepareInput = { generationInputFactory.create(document) },
            )
        generationTasks.validate(context)
        val result = generationTasks.render(context)
        if (result.isEmpty()) {
            throw NoArtifactsGenerated()
        }
        FileSystemGeneratedArtifactWriter(
            outputDirectory = generatorConfiguration.output.outputDirectory,
            manifest = generatorConfiguration.output.manifest,
        ).write(result)
    }
}
