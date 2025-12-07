package com.tietoevry.banking.asyncapi.generator.core.helpers

import com.tietoevry.banking.asyncapi.generator.core.repository.SourceRepository

class SourceRepositoryPrinter(private val sourceRepository: SourceRepository) {

    fun printSources() {
        val sources = sourceRepository.getAllSources()

        if (sources.isEmpty()) {
            println("No registered YAML sources.")
            return
        }

        println("Registered YAML sources (${sources.size} files):\n")

        sources.forEach { source ->
            println(
                "- ${source.file.absolutePath}  " +
                    "[id=${source.id}]  " +
                    "(${source.lines.size} lines)"
            )
        }
    }

    fun printLineMap() {
        val lineMap = sourceRepository.getAllLines()
        if (lineMap.isEmpty()) {
            println("No registered YAML path → line mappings.")
            return
        }

        println("YAML path → line mappings (${lineMap.size} entries):\n")

        lineMap
            .toSortedMap()
            .forEach { (path, line) ->
                println("  - $path → line $line")
            }
    }

    fun printSnippetsForAllPaths(contextLines: Int = 1) {
        val lineMap = sourceRepository.getAllLines()

        if (lineMap.isEmpty()) {
            println("No paths registered for snippets.")
            return
        }

        println("Snippets for all registered paths:\n")

        lineMap
            .toSortedMap()
            .forEach { (path, _) ->
                val snippet = sourceRepository.pathSnippet(path, contextLines)
                println("Path: $path")
                println(snippet)
                println()
            }
    }
}
