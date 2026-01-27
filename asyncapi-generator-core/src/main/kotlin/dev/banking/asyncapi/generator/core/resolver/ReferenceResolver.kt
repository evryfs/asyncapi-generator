package dev.banking.asyncapi.generator.core.resolver

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.validator.util.ValidationResults

class ReferenceResolver(
    val asyncApiContext: AsyncApiContext,
) {

    fun resolve(reference: Reference, contextString: String, results: ValidationResults) {
        asyncApiContext.findReference(reference)?.let { retrievedReference ->
            reference.model = retrievedReference
            return
        }
        results.error(
            "$contextString reference '${reference.ref}' could not be resolved",
            asyncApiContext.getLine(reference, reference::ref),
            )
    }
}
