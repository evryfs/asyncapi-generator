package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

class AsyncApiExternalContext(
    val context: AsyncApiContext,
) {
    private val loadedFiles = mutableSetOf<String>() // absolute paths

    fun loadExternal(reference: Reference) {
        val clean = reference.ref.trim().trimStart('\'', '"', '|', '>')
        if (clean.isEmpty()) {
            return
        }
        if (clean.startsWith("#")) {
            return
        }
        val docPart = clean.substringBefore('#').trim()
        if (docPart.isEmpty()) {
            return
        }
        val baseFile =
            reference.sourceId
                ?.let(context::findFileById)
                ?: context.getCurrentFile()
        val externalFile = File(baseFile.parentFile, docPart).canonicalFile
        val key = externalFile.absolutePath
        if (!loadedFiles.add(key)) {
            return
        }
        val rootNode = AsyncApiRegistry.readYaml(externalFile, context)

        if (rootNode.optional("asyncapi") != null) {
            val parser = AsyncApiParser(context)
            val parsed = parser.parse(rootNode)
            val result = AsyncApiValidator(context).validate(parsed)
            result.logWarnings()
            result.throwErrors()
        } else {
            ExternalFragmentProcessor(context).parseAndValidate(
                rootNode = rootNode,
                reference = reference,
            )
        }
    }
}
