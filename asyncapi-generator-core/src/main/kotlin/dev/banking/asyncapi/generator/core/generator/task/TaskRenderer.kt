package dev.banking.asyncapi.generator.core.generator.task

import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.plan.GenerationTask

/**
 * Checks and renders one kind of planned task. Every task of a plan is checked before any task is
 * rendered, so a plan either renders completely or fails before anything is written.
 */
internal interface TaskRenderer<in T : GenerationTask> {
    /** Fails when [task] cannot render the contract of [context]. */
    fun validate(
        task: T,
        context: GenerationContext,
    ) = Unit

    /** The artifacts [task] renders from [context]. */
    fun render(
        task: T,
        context: GenerationContext,
    ): GenerationResult
}
