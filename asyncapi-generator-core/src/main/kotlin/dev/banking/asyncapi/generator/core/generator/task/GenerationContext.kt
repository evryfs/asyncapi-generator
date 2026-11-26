package dev.banking.asyncapi.generator.core.generator.task

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * What planned tasks render from: the bundled document, the plan, and the generation input, which
 * is prepared from the document only when a task first needs it. A plan that only writes documents
 * therefore never analyzes schemas.
 *
 * @param document the bundled AsyncAPI document
 * @param plan the planned tasks
 * @param prepareInput prepares the generation input from [document]
 */
internal class GenerationContext(
    val document: AsyncApiDocument,
    val plan: GenerationPlan,
    prepareInput: () -> GenerationInput,
) {
    private val preparedInput = lazy(prepareInput)

    /** The analyzed schemas and channels of [document]. */
    val input: GenerationInput by preparedInput

    /** Whether a task has needed [input] so far. */
    val isInputPrepared: Boolean
        get() = preparedInput.isInitialized()

    /** Whether the plan has a task of type [T]. */
    inline fun <reified T> plans(): Boolean = plan.tasks.any { it is T }
}
