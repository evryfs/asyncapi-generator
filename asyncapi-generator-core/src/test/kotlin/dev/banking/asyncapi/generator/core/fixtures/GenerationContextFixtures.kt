package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.plan.GenerationPlan
import dev.banking.asyncapi.generator.core.generator.task.GenerationContext
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.info.Info

/** A [GenerationContext] whose input is [input]; the document matters only to document tasks. */
internal fun generationContext(
    input: GenerationInput = generationInputOf(emptyMap()),
    plan: GenerationPlan = GenerationPlan(emptyList()),
    document: AsyncApiDocument = AsyncApiDocument(asyncapi = "3.0.0", info = Info(title = "Test", version = "1.0.0")),
): GenerationContext = GenerationContext(document = document, plan = plan, prepareInput = { input })
