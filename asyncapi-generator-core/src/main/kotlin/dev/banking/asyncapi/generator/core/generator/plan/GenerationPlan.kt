package dev.banking.asyncapi.generator.core.generator.plan

/**
 * Ordered generation work selected before rendering or writing artifacts.
 *
 * @param tasks the tasks in the order their artifacts are rendered.
 */
data class GenerationPlan(
    val tasks: List<GenerationTask>,
)
