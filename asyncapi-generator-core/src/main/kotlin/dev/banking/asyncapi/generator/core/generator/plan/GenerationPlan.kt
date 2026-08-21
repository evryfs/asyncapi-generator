package dev.banking.asyncapi.generator.core.generator.plan

/**
 * Ordered generation work selected before rendering or writing artifacts.
 */
data class GenerationPlan(
    val tasks: List<GenerationTask>,
)
