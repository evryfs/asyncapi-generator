package com.tietoevry.banking.asyncapi.generator.core.helpers

import com.tietoevry.banking.asyncapi.generator.core.repository.ModelRepository
import com.tietoevry.banking.asyncapi.generator.core.model.references.Reference

class ModelRepositoryPrinter(private val modelRepository: ModelRepository) {

    fun printAll() {
        val entries = modelRepository.getModelsByInstance().values

        if (entries.isEmpty()) {
            println("🗂️  No registered models.")
            return
        }

        println("🗂️  Registered models (${entries.size} total):\n")

        entries.reversed().forEachIndexed { index, entry ->
            val modelName = entry.model::class.simpleName ?: "UnknownModel"
            val fieldLabel = entry.fieldName?.let { " – \"$it\"" } ?: ""
            println("${index + 1}. $modelName$fieldLabel (${entry.fieldLines.size} fields)")
            entry.fieldLines.forEach { (key, line) ->
                println("   - $key → line $line")
            }
            println()
        }
    }

    fun printPaths() {
        val paths = modelRepository.getModelsByPath() // Need accessor
        println("🧭 Registered YAML model paths (${paths.size} total):")
        paths.forEach { (path, model) ->
            println("  - $path → modelName:${model::class.simpleName}, model:${model}")
        }
    }

    fun printReferences() {
        val references = modelRepository.getModelsByInstance().keys.filterIsInstance<Reference>()
        if (references.isEmpty()) {
            println("🔗 No registered Reference objects.")
            return
        }

        println("🔗 Registered Reference objects (${references.size} total):\n")
        references.forEach { ref ->
            val modelName = ref.model?.let { it::class.simpleName } ?: "(unresolved)"
            println("  - ref=${ref.ref} → name=$modelName")
        }
        println()
    }
}
