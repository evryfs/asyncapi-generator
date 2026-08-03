package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File
import java.io.IOException
import java.net.URISyntaxException

internal class AsyncApiExternalContext(
    val context: AsyncApiContext,
) {
    private data class FragmentIdentity(
        val file: String,
        val pointer: String,
        val category: String,
        val parserProfile: String,
    )

    private val documents = mutableMapOf<String, ParserNode>()
    private val loadedDocuments = mutableSetOf<String>()
    private val loadedFragments = mutableSetOf<FragmentIdentity>()
    private val pendingFragmentValidations = mutableListOf<() -> Unit>()
    private val pathResolver = ExternalReferencePathResolver()
    private var fragmentLoadDepth = 0

    fun loadExternal(reference: Reference) {
        val referenceOrigin = context.modelRepository.getReferenceOrigin(reference)
        val sourceFile =
            referenceOrigin?.file
                ?: reference.sourceId?.let(context::findFileById)
                ?: context.getCurrentFile()
        val resolved = try {
            resolveReference(reference, sourceFile)
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

        val referenceLocation = referenceLocation(reference)
        val documentKey =
            try {
                context.registerExternalDocument(externalFile, referenceLocation)
                externalFile.canonicalPath
            } catch (exception: IOException) {
                throw missingDocument(reference, externalFile)
            } catch (exception: SecurityException) {
                throw missingDocument(reference, externalFile)
            }
        val rootNode = documents.getOrPut(documentKey) {
            AsyncApiRegistry.read(externalFile, context)
        }
        try {
            context.registerReferenceTarget(
                file = externalFile,
                pointer = resolved.pointer.toString(),
                location = referenceLocation,
            )
        } catch (exception: IOException) {
            throw missingDocument(reference, externalFile)
        } catch (exception: SecurityException) {
            throw missingDocument(reference, externalFile)
        }
        val isAsyncApiDocument =
            (rootNode.node as? DocumentObject)?.member("asyncapi") != null
        if (isAsyncApiDocument) {
            if (ExternalReferenceTargetResolver.resolve(rootNode, resolved.pointer) == null) {
                throw missingTarget(reference)
            }
            if (!loadedDocuments.add(documentKey)) return
            context.withinExternalReference(referenceLocation) {
                val parser = AsyncApiParser(context)
                val parsed = parser.parse(rootNode)
                val result = AsyncApiValidator(context).validate(parsed)
                ValidationReporter(context).throwErrors(result)
                context.collectExternalValidationWarnings(result.warnings)
            }
        } else {
            val parserProfile = referenceOrigin?.parserProfile
            val profiledRoot = parserProfile?.let(rootNode::withProfile) ?: rootNode
            val target = ExternalReferenceTargetResolver.resolve(profiledRoot, resolved.pointer)
                ?: throw missingTarget(reference)
            val fragmentIdentity = FragmentIdentity(
                file = documentKey,
                pointer = resolved.pointer.toString(),
                category = reference.referenceCategoryKey?.name.orEmpty(),
                parserProfile = parserProfile?.name.orEmpty(),
            )
            if (!loadedFragments.add(fragmentIdentity)) return
            context.withinExternalReference(referenceLocation) {
                parseFragment(target, reference)
            }
        }
    }

    private fun parseFragment(
        target: ExternalReferenceTargetResolver.Target,
        reference: Reference,
    ) {
        var parsed = false
        fragmentLoadDepth++
        try {
            pendingFragmentValidations += ExternalFragmentProcessor(context).parseAndDeferValidation(
                target = target,
                reference = reference,
            )
            parsed = true
        } finally {
            fragmentLoadDepth--
            if (fragmentLoadDepth == 0) {
                val validations = pendingFragmentValidations.toList()
                pendingFragmentValidations.clear()
                if (parsed) validations.forEach { it() }
            }
        }
    }

    private fun resolveReference(
        reference: Reference,
        sourceFile: File,
    ): ExternalReferencePathResolver.ResolvedReference? {
        val parsed = reference.ref.parseReference()
        if (parsed.isExternal) {
            return pathResolver.resolve(reference.ref, sourceFile)
        }

        val canonicalSource = sourceFile.canonicalFile
        val sourceRoot = documents[canonicalSource.absolutePath] ?: return null
        val sourceIsAsyncApiDocument =
            (sourceRoot.node as? DocumentObject)?.member("asyncapi") != null
        if (sourceIsAsyncApiDocument) return null

        return ExternalReferencePathResolver.ResolvedReference(
            file = canonicalSource,
            pointer = parsed.pointer,
        )
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
