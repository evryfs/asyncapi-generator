package dev.banking.asyncapi.generator.core.context

import dev.banking.asyncapi.generator.core.model.references.Reference
import dev.banking.asyncapi.generator.core.parser.node.ParserNode
import dev.banking.asyncapi.generator.core.repository.ModelRepository
import dev.banking.asyncapi.generator.core.repository.SourceRepository
import java.io.File
import kotlin.reflect.KProperty0

class AsyncApiContext {
    val sourceRepository = SourceRepository()
    val modelRepository = ModelRepository(sourceRepository)

    val externalLoader = AsyncApiExternalContext(this)

    fun register(model: Any, node: ParserNode) {
        modelRepository.register(model, node)

        if (model is Reference) {
            externalLoader.loadExternal(model.ref)
        }
    }

    fun registerSource(file: File, content: String) {
        sourceRepository.registerSource(file, content)
    }

    fun registerLine(path: String, line: Int) {
        sourceRepository.registerLine(path, line)
    }

    fun <R> getLine(model: Any, property: KProperty0<R>): Int? {
        return modelRepository.getLine(model, property)
    }

    fun pathSnippet(path: String, contextLines: Int = 3): String {
        return sourceRepository.pathSnippet(path, contextLines)
    }

    fun validatorSnippet(line: Int, contextLines: Int = 3): String {
        return sourceRepository.lineSnippet(line, contextLines)
    }

    fun findReference(reference: Reference): Any? {
        return modelRepository.findByReference(reference)
    }

    fun getCurrentFile(): File {
        return sourceRepository.getCurrentFile()
    }
}
