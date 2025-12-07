package com.tietoevry.banking.asyncapi.generator.core.resolver

import com.tietoevry.banking.asyncapi.generator.core.context.AsyncApiContext
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference
import com.tietoevry.banking.asyncapi.generator.core.validator.util.ValidationResults

class ReferenceResolver(
    val asyncApiContext: AsyncApiContext,
) {

    fun resolve(name: String, reference: Reference, contextString: String, results: ValidationResults) {
        asyncApiContext.findReference(reference)?.let { retrievedReference ->
            reference.model = retrievedReference
            return
        }
        results.error(
            "$contextString reference '${reference.ref}' could not be resolved on: $name",
            asyncApiContext.getLine(reference, reference::ref),
            )
    }
}
