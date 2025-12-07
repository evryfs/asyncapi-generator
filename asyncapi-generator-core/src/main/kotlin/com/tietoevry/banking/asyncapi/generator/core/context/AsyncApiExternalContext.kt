package com.tietoevry.banking.asyncapi.generator.core.context

import com.tietoevry.banking.asyncapi.generator.core.parser.AsyncApiParser
import com.tietoevry.banking.asyncapi.generator.core.registry.AsyncApiRegistry
import com.tietoevry.banking.asyncapi.generator.core.validator.AsyncApiValidator
import java.io.File

class AsyncApiExternalContext(private val context: AsyncApiContext) {

    private val loadedFiles = mutableSetOf<String>()  // absolute paths

    fun loadExternal(ref: String) {
        val clean = ref.trim().trimStart('\'', '"', '|', '>')
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
        val baseFile = context.getCurrentFile()
        val externalFile = File(baseFile.parentFile, docPart).canonicalFile
        val key = externalFile.absolutePath
        if (!loadedFiles.add(key)) {
            return
        }
        val rootNode = AsyncApiRegistry.readYaml(externalFile, context) // Pass the context down

        val parser = AsyncApiParser(context)
        val parsed = parser.parse(rootNode)

        val validator = AsyncApiValidator(context) // Pass context to validator
        val result = validator.validate(parsed)

        result.throwWarnings()
        result.throwErrors()
    }
}
