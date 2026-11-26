package dev.banking.asyncapi.generator.core.generator.artifact

import dev.banking.asyncapi.generator.core.generator.input.PayloadFormatChecks
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.protobuf.MavenRepositoryProtocProvider
import dev.banking.asyncapi.generator.core.generator.protobuf.NativeProtobufGenerator
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocProvider
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer

/**
 * Renders planned native Protobuf artifacts, which need at least one native Protobuf schema.
 *
 * @param protocProvider supplies `protoc` for compiling Protobuf models
 */
internal class NativeProtobufArtifactGeneration(
    protocProvider: ProtocProvider = MavenRepositoryProtocProvider(),
) : TaskRenderer<GenerationTask.NativeProtobufArtifacts> {
    private val nativeProtobufGenerator = NativeProtobufGenerator(protocProvider)

    override fun validate(
        task: GenerationTask.NativeProtobufArtifacts,
        context: GenerationContext,
    ) {
        PayloadFormatChecks.requireNativeSchema(
            output = "Native Protobuf generation",
            generationInput = context.input,
            supportedInput = "native Protobuf schemas",
            isSupported = { schema -> schema.format.isNativeProtobuf },
        )
    }

    override fun render(
        task: GenerationTask.NativeProtobufArtifacts,
        context: GenerationContext,
    ): GenerationResult =
        nativeProtobufGenerator
            .render(
                schemas = context.input.multiFormatSchemas,
                models = task.schema.models,
            ).inSchemaPackage(task.schema.schemaPackageName)
}
