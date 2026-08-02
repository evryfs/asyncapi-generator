package dev.banking.asyncapi.generator.core.resolver

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.validator.ValidationRule.REFERENCE_UNRESOLVED
import dev.banking.asyncapi.generator.core.validator.util.ValidationCollector

class ReferenceResolver(
    val asyncApiContext: AsyncApiContext,
) {

    fun resolve(reference: Reference, contextString: String, results: ValidationCollector) {
        asyncApiContext.findReference(reference)?.let { retrievedReference ->
            reference.model = retrievedReference
            return
        }
        results.error(
            REFERENCE_UNRESOLVED,
            "$contextString reference '${reference.ref}' could not be resolved",
            sourceLocation = asyncApiContext.getSourceLocation(reference, reference::ref),
        )
    }
}
