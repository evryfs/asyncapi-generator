package dev.banking.asyncapi.generator.core.context

import java.io.File

/**
 * Resolves external reference document paths against the source file that owns
 * the reference.
 *
 * Expected behavior is covered by:
 * - `SchemaParserTest`
 */
class ExternalReferencePathResolver(
    private val context: AsyncApiContext,
) {
    fun resolveFile(
        reference: String,
        sourceId: String?,
    ): File? {
        val clean = reference.trim().trimStart('\'', '"', '|', '>')
        if (clean.isEmpty()) {
            return null
        }
        if (clean.startsWith("#")) {
            return null
        }

        val docPart = clean.substringBefore('#').trim()
        if (docPart.isEmpty()) {
            return null
        }

        val baseFile =
            sourceId
                ?.let(context::findFileById)
                ?: context.getCurrentFile()

        return File(baseFile.parentFile, docPart).canonicalFile
    }
}
