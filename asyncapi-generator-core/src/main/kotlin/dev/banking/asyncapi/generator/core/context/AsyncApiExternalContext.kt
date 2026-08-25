package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.document.DocumentObject
import dev.banking.asyncapi.generator.core.document.SourceLocation
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.model.validator.ValidationFinding
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import dev.banking.asyncapi.generator.core.validator.util.ValidationReporter
import java.io.File
import java.io.IOException
import java.net.URISyntaxException

/**
 * Handles loading and parsing of external document references.
 *
 * @param sourceTracking source file tracking for resolving file references
 * @param modelTracking model tracking for source locations and reference origins
 * @param warningCollector collector for external validation warnings
 * @param registerExternalDocument registers an external document with the resource budget
 * @param registerReferenceTarget registers a reference target with the resource budget
 * @param withinExternalReference executes a block within an external reference scope
 * @param createParser factory for creating a new parser instance
 * @param createValidator factory for creating a new validator instance
 * @param createReporter factory for creating a new validation reporter instance
 * @param createFragmentProcessor factory for creating a new fragment processor instance
 * @param createDiagnosticException factory for creating parser diagnostic exceptions
 * @param readDocument reads and parses a document file into a parser node
 */
internal class AsyncApiExternalContext(
    private val sourceTracking: SourceTracking,
    private val modelTracking: ModelTracking,
    private val warningCollector: ValidationWarningCollector,
    private val registerExternalDocument: (File, SourceLocation) -> Unit,
    private val registerReferenceTarget: (File, String, SourceLocation) -> Unit,
    private val withinExternalReference: (SourceLocation, () -> Unit) -> Unit,
    private val createParser: () -> AsyncApiParser,
    private val createValidator: () -> AsyncApiValidator,
    private val createReporter: () -> ValidationReporter,
    private val createFragmentProcessor: () -> ExternalFragmentProcessor,
    private val createDiagnosticException: (ParserDiagnostic) -> AsyncApiParseException,
    private val readDocument: (File) -> ParserNode,
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
        val referenceOrigin = modelTracking.getReferenceOrigin(reference)
        val sourceFile =
            referenceOrigin?.file
                ?: reference.sourceId?.let(sourceTracking::findFileById)
                ?: sourceTracking.getCurrentFile()
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
                registerExternalDocument(externalFile, referenceLocation)
                externalFile.canonicalPath
            } catch (exception: IOException) {
                throw missingDocument(reference, externalFile)
            } catch (exception: SecurityException) {
                throw missingDocument(reference, externalFile)
            }
        val rootNode = documents.getOrPut(documentKey) {
            readDocument(externalFile)
        }
        try {
            registerReferenceTarget(
                externalFile,
                resolved.pointer.toString(),
                referenceLocation,
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
            withinExternalReference(referenceLocation) {
                val parser = createParser()
                val parsed = parser.parse(rootNode)
                val result = createValidator().validate(parsed)
                createReporter().throwErrors(result)
                warningCollector.collect(result.warnings)
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
            withinExternalReference(referenceLocation) {
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
            pendingFragmentValidations += createFragmentProcessor().parseAndDeferValidation(
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
    ): AsyncApiParseException = createDiagnosticException(diagnostic)

    private fun referenceLocation(reference: Reference) =
        requireNotNull(
            modelTracking.getSourceLocation(reference, $$"$ref")
                ?: modelTracking.getSourceLocation(reference),
        ) { "Reference '${reference.ref}' was not registered with a source location" }
}
