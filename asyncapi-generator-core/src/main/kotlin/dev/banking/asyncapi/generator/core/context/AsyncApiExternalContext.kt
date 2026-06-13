package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.AsyncApiParser
import dev.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import dev.banking.asyncapi.generator.core.validator.AsyncApiValidator

class AsyncApiExternalContext(
    val context: AsyncApiContext,
) {
    private val loadedFiles = mutableSetOf<String>() // absolute paths
    private val pathResolver = ExternalReferencePathResolver(context)

    fun loadExternal(reference: Reference) {
        val externalFile = pathResolver.resolveFile(reference.ref, reference.sourceId) ?: return
        val key = externalFile.absolutePath
        if (!loadedFiles.add(key)) {
            return
        }
        val rootNode = AsyncApiRegistry.read(externalFile, context)

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
