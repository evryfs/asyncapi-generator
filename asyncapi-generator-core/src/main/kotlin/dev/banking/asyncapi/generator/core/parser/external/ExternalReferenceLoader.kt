package dev.banking.asyncapi.generator.core.parser.external

import com.fasterxml.jackson.core.JsonPointer
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.model.diagnostics.ParserDiagnostic
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiParseException
import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.model.references.parseReference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.DocumentObject
import dev.banking.asyncapi.generator.core.source.SourceLocation
import dev.banking.asyncapi.generator.core.source.SourceReader
import java.io.File
import java.io.IOException
import java.net.URISyntaxException

/**
 * Loads the documents and fragments that the references of one load point at.
 *
 * References are followed depth first in the order the parser met them: the target of a reference
 * is parsed, then the references inside that target are followed, before the next reference. An
 * external AsyncAPI document is parsed as a whole, once; any other target is parsed as a fragment of
 * the kind its reference expects, once per kind and parser profile.
 *
 * @param context the load whose references are followed
 */
internal class ExternalReferenceLoader(
    private val context: AsyncApiContext,
) {
    /**
     * What makes two fragment loads the same: the same node parsed as the same kind with the same profile.
     *
     * @property file the canonical path of the fragment's file
     * @property pointer the JSON pointer of the fragment
     * @property category the kind of object the fragment is parsed as
     * @property parserProfile the parser profile the fragment is parsed with
     */
    private data class FragmentIdentity(
        val file: String,
        val pointer: String,
        val category: String,
        val parserProfile: String,
    )

    /**
     * The document and node a reference points at.
     *
     * @property documentKey the canonical path of the document
     * @property pointer the JSON pointer within the document
     * @property root the root node of the document
     */
    private class Target(
        val documentKey: String,
        val pointer: JsonPointer,
        val root: ParserNode,
    )

    private val documents = mutableMapOf<String, ParserNode>()
    private val loadedDocuments = mutableSetOf<String>()
    private val loadedFragments = mutableSetOf<FragmentIdentity>()
    private val loaded = mutableListOf<LoadedTarget>()

    /**
     * Follows every reference parsed so far and the references found in their targets.
     *
     * @return the loaded documents and fragments, each listed after the targets it references
     */
    fun loadAll(): List<LoadedTarget> {
        context.parsedReferences.since(0).forEach(::follow)
        return loaded.toList()
    }

    private fun follow(entry: ParsedReferences.Entry) {
        val target = target(entry.reference) ?: return
        if ((target.root.node as? DocumentObject)?.member("asyncapi") != null) {
            followDocument(entry.reference, target)
        } else {
            followFragment(entry, target)
        }
    }

    private fun followDocument(
        reference: Reference,
        target: Target,
    ) {
        target.root.resolvePointer(target.pointer) ?: throw context.missingTarget(reference)
        if (!loadedDocuments.add(target.documentKey)) return
        context.sourceBudget.withinExternalReference(context.referenceLocation(reference)) {
            val document = withNewReferences { AsyncApiParser(context).parse(target.root) }
            loaded += LoadedTarget.Document(document)
        }
    }

    private fun followFragment(
        entry: ParsedReferences.Entry,
        target: Target,
    ) {
        val reference = entry.reference
        val category = checkNotNull(reference.referenceCategoryKey) { "Reference '${reference.ref}' has no category" }
        val profile = context.modelTracking.getReferenceOrigin(reference)?.profile
        val node =
            (profile?.let(target.root::withProfile) ?: target.root).resolvePointer(target.pointer)
                ?: throw context.missingTarget(reference)
        val identity =
            FragmentIdentity(target.documentKey, target.pointer.toString(), category.name, profile?.name.orEmpty())
        if (!loadedFragments.add(identity)) return
        context.sourceBudget.withinExternalReference(context.referenceLocation(reference)) {
            val model = withNewReferences { entry.parseTarget(node) }
            loaded += LoadedTarget.Fragment(category, node.label, model)
        }
    }

    /** Runs [parse], then follows the references it parsed. */
    private fun <T> withNewReferences(parse: () -> T): T {
        val first = context.parsedReferences.size
        return parse().also { context.parsedReferences.since(first).forEach(::follow) }
    }

    /** The document and pointer [reference] points at, or `null` when its target needs no loading. */
    private fun target(reference: Reference): Target? {
        val (file, pointer) = resolve(reference) ?: return null
        if (!file.isFile || !file.canRead()) throw context.missingDocument(reference, file)
        val location = context.referenceLocation(reference)
        val documentKey =
            context.guardFileAccess(reference, file) {
                context.sourceBudget.registerExternalDocument(file, location)
                file.canonicalPath
            }
        val root =
            documents.getOrPut(documentKey) {
                ParserNodeFactory.root(SourceReader.read(file, context.sourceLimits), context)
            }
        context.guardFileAccess(reference, file) {
            context.sourceBudget.registerReferenceTarget(file, pointer.toString(), location)
        }
        return Target(documentKey, pointer, root)
    }

    private fun resolve(reference: Reference): Pair<File, JsonPointer>? =
        try {
            val parsed = reference.ref.parseReference()
            val sourceFile = sourceFile(reference)
            val file =
                if (parsed.isExternal) {
                    requireNotNull(parsed.resolveDocumentAgainst(sourceFile))
                } else {
                    localFragmentDocument(sourceFile)
                }
            file?.let { it to parsed.pointer }
        } catch (exception: URISyntaxException) {
            throw context.invalidReference(reference, exception.reason, exception)
        } catch (exception: IllegalArgumentException) {
            throw context.invalidReference(reference, exception.message ?: "invalid reference", exception)
        } catch (exception: IOException) {
            throw context.missingDocument(reference, File(reference.ref.substringBefore('#')), exception)
        }

    private fun sourceFile(reference: Reference): File =
        checkNotNull(context.modelTracking.getReferenceOrigin(reference)) {
            "Reference '${reference.ref}' was not registered"
        }.file

    /**
     * The document of a local reference written in a fragment file, or `null` when the reference is
     * written in an AsyncAPI document, whose local targets are parsed with the document.
     */
    private fun localFragmentDocument(sourceFile: File): File? {
        val canonicalSource = sourceFile.canonicalFile
        val sourceRoot = documents[canonicalSource.absolutePath] ?: return null
        return canonicalSource.takeIf { (sourceRoot.node as? DocumentObject)?.member("asyncapi") == null }
    }
}
