package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File
import java.io.IOException
import java.net.URISyntaxException

class AsyncApiExternalContext(
    val context: AsyncApiContext,
) {
    private data class FragmentIdentity(
        val file: String,
        val pointer: String,
        val category: String,
    )

    private val documents = mutableMapOf<String, ParserNode>()
    private val loadedDocuments = mutableSetOf<String>()
    private val loadedFragments = mutableSetOf<FragmentIdentity>()
    private val pathResolver = ExternalReferencePathResolver()

    fun loadExternal(reference: Reference) {
        val resolved = try {
            val sourceFile =
                context.modelRepository.getReferenceOrigin(reference)?.file
                    ?: reference.sourceId?.let(context::findFileById)
                    ?: context.getCurrentFile()
            pathResolver.resolve(
                reference.ref,
                sourceFile,
            )
        } catch (exception: URISyntaxException) {
            throw invalidReference(reference, exception.reason)
        } catch (exception: IllegalArgumentException) {
            throw invalidReference(reference, exception.message ?: "invalid reference")
        } catch (exception: IOException) {
            throw missingDocument(reference, File(reference.ref.substringBefore('#')))
        }
        if (resolved == null) return

        val externalFile = resolved.file
        if (!externalFile.isFile || !externalFile.canRead()) {
            throw missingDocument(reference, externalFile)
        }

        val documentKey = externalFile.canonicalPath
        val rootNode = documents.getOrPut(documentKey) {
            AsyncApiRegistry.read(externalFile, context)
        }
        val target = ExternalReferenceTargetResolver.resolve(rootNode, resolved.pointer)
            ?: throw missingTarget(reference)

        val isAsyncApiDocument =
            (rootNode.node as? DocumentObject)?.member("asyncapi") != null
        if (isAsyncApiDocument) {
            if (!loadedDocuments.add(documentKey)) return
            val parser = AsyncApiParser(context)
            val parsed = parser.parse(rootNode)
            val result = AsyncApiValidator(context).validate(parsed)
            result.logWarnings()
            result.throwErrors()
        } else {
            val fragmentIdentity = FragmentIdentity(
                file = documentKey,
                pointer = resolved.pointer.toString(),
                category = reference.referenceCategoryKey?.name.orEmpty(),
            )
            if (!loadedFragments.add(fragmentIdentity)) return
            ExternalFragmentProcessor(context).parseAndValidate(
                target = target,
                reference = reference,
            )
        }
    }

    private fun invalidReference(reference: Reference, reason: String): AsyncApiParseException =
        parserFailure(
            ParserDiagnostic.InvalidReference(
                reference = reference.ref,
                reason = reason.trimEnd('.'),
                path = referenceLocation(reference).path,
                sourceLocation = referenceLocation(reference),
            ),
        )

    private fun missingDocument(reference: Reference, file: File): AsyncApiParseException =
        parserFailure(
            ParserDiagnostic.ReferenceDocumentNotFound(
                reference = reference.ref,
                resolvedFile = file.absolutePath,
                path = referenceLocation(reference).path,
                sourceLocation = referenceLocation(reference),
            ),
        )

    private fun missingTarget(reference: Reference): AsyncApiParseException =
        parserFailure(
            ParserDiagnostic.ReferenceTargetNotFound(
                reference = reference.ref,
                path = referenceLocation(reference).path,
                sourceLocation = referenceLocation(reference),
            ),
        )

    private fun parserFailure(
        diagnostic: ParserDiagnostic,
    ): AsyncApiParseException =
        AsyncApiParseException.ParserDiagnosticFailure(diagnostic, context)

    private fun referenceLocation(reference: Reference) =
        requireNotNull(
            context.getSourceLocation(reference, $$"$ref")
                ?: context.getSourceLocation(reference),
        ) { "Reference '${reference.ref}' was not registered with a source location" }
}
