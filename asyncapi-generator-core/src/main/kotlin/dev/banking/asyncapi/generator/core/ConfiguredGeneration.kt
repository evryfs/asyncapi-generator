package dev.banking.asyncapi.generator.core

import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlanner
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask
import java.io.File

/**
 * Settings that passed every check, and what generating with them will produce.
 *
 * Build plugins use it before generating, to tell their build which directories hold sources and
 * resources, and whether `protoc` is needed.
 *
 * @param configuration the checked configuration
 */
class ConfiguredGeneration internal constructor(
    internal val configuration: GeneratorConfiguration,
) {
    private val plan = GenerationPlanner().plan(configuration)

    /** The configuration warnings, formatted for the user; empty when there are none. */
    val warnings: String = configuration.formatWarnings().trimEnd()

    /** The directory that sources and schemas are written to. */
    val outputDirectory: File = configuration.output.outputDirectory

    /** Whether sources, which the build should compile, are written to [outputDirectory]. */
    val writesSources: Boolean = GeneratedArtifactKind.SOURCE in plan.outputKinds

    /** Whether schemas, which the build should package as resources, are written to [outputDirectory]. */
    val writesSchemas: Boolean = GeneratedArtifactKind.SCHEMA in plan.outputKinds

    /** Whether generating runs `protoc`, which only native Protobuf models do. */
    val requiresProtoc: Boolean =
        plan.tasks.any { task -> task is GenerationTask.NativeProtobufArtifacts && task.schema.models != null }
}
