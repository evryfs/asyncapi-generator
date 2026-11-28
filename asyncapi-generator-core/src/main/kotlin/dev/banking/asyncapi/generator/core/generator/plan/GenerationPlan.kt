package dev.banking.asyncapi.generator.core.generator.plan

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind

/**
 * Ordered generation work selected before rendering or writing artifacts.
 *
 * @param tasks the tasks in the order their artifacts are rendered.
 */
internal data class GenerationPlan(
    val tasks: List<GenerationTask>,
) {
    /** The kinds of artifact any task writes under the output directory. */
    val outputKinds: Set<GeneratedArtifactKind>
        get() = tasks.flatMapTo(mutableSetOf(), GenerationTask::outputKinds)
}
