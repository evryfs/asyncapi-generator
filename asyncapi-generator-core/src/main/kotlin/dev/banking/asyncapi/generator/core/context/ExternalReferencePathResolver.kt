package dev.banking.asyncapi.generator.core.context

import com.fasterxml.jackson.core.JsonPointer
import dev.banking.asyncapi.generator.core.model.references.parseReference
import java.io.File

/**
 * Resolves external reference document paths against the source file that owns
 * the reference.
 */
internal class ExternalReferencePathResolver {
    data class ResolvedReference(
        val file: File,
        val pointer: JsonPointer,
    )

    fun resolve(
        reference: String,
        sourceFile: File,
    ): ResolvedReference? {
        val parsed = reference.parseReference()
        if (!parsed.isExternal) return null
        val file = requireNotNull(parsed.resolveDocumentAgainst(sourceFile))
        return ResolvedReference(file, parsed.pointer)
    }
}
