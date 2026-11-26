package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.generator.artifact.AvroSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.DocumentArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.JsonSchemaArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.KafkaKeyModelArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.ModelArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeAvroArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.artifact.NativeProtobufArtifactGeneration
import dev.banking.asyncapi.generator.core.generator.input.PayloadFormatChecks
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientGeneration
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import dev.banking.asyncapi.generator.core.generator.protobuf.ProtocProvider
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.generator.task.TaskRenderer

/**
 * Runs the tasks of a plan, each with its renderer: every task is checked first, then every task is
 * rendered, in plan order.
 *
 * @param protocProvider supplies `protoc` for native Protobuf models
 */
internal class GenerationTasks(
    protocProvider: ProtocProvider,
) {
    private val documents = DocumentArtifactGeneration()
    private val models = ModelArtifactGeneration(SourceTargets::of)
    private val kafkaKeyModels = KafkaKeyModelArtifactGeneration(models)
    private val springKafkaClients = SpringKafkaClientGeneration { language -> SourceTargets.of(language).clientSyntax }
    private val avroSchemas = AvroSchemaArtifactGeneration()
    private val nativeAvro = NativeAvroArtifactGeneration()
    private val nativeProtobuf = NativeProtobufArtifactGeneration(protocProvider)
    private val jsonSchemas = JsonSchemaArtifactGeneration()

    /**
     * Fails when a task cannot render the contract of [context], or when a native payload is
     * handled by no task of the plan.
     */
    fun validate(context: GenerationContext) {
        context.plan.tasks.forEach { task -> bind(task).validate(context) }
        if (context.isInputPrepared) PayloadFormatChecks.rejectUnhandledFormats(context.input, context.plan)
    }

    /** The artifacts of every task of the plan. */
    fun render(context: GenerationContext): GenerationResult =
        context.plan.tasks.fold(GenerationResult.Empty) { result, task -> result + bind(task).render(context) }

    private fun bind(task: GenerationTask): BoundTask<*> =
        when (task) {
            is GenerationTask.DocumentArtifact -> BoundTask(task, documents)
            is GenerationTask.ModelArtifacts -> BoundTask(task, models)
            is GenerationTask.KafkaKeyModelArtifacts -> BoundTask(task, kafkaKeyModels)
            is GenerationTask.SpringKafkaClient -> BoundTask(task, springKafkaClients)
            is GenerationTask.AvroSchemaArtifacts -> BoundTask(task, avroSchemas)
            is GenerationTask.NativeAvroArtifacts -> BoundTask(task, nativeAvro)
            is GenerationTask.NativeProtobufArtifacts -> BoundTask(task, nativeProtobuf)
            is GenerationTask.JsonSchemaArtifacts -> BoundTask(task, jsonSchemas)
        }

    /**
     * A task with the renderer of its type.
     *
     * @param T the type of the task
     * @param task the planned task
     * @param renderer the renderer of tasks of its type
     */
    private class BoundTask<T : GenerationTask>(
        private val task: T,
        private val renderer: TaskRenderer<T>,
    ) {
        fun validate(context: GenerationContext) = renderer.validate(task, context)

        fun render(context: GenerationContext): GenerationResult = renderer.render(task, context)
    }
}
