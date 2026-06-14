package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Locale
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.fail

/**
 * Compiles generated Java source artifacts inside tests.
 *
 * Use this fixture when a generator test needs to prove that generated Java
 * output is structurally valid Java code against the current test classpath,
 * without adding a full sample application to the repository.
 */
internal class GeneratedJavaCompiler(
    private val classpath: String = System.getProperty("java.class.path"),
) {
    fun compile(
        artifacts: Iterable<GeneratedArtifact>,
        workspace: Path,
    ): GeneratedJavaCompilation {
        val javaArtifacts = artifacts.filter { artifact -> artifact.kind == GeneratedArtifactKind.JAVA_SOURCE }
        require(javaArtifacts.isNotEmpty()) {
            "Expected at least one Java source artifact to compile"
        }

        val sourceDirectory = workspace.resolve("generated-java-sources").createDirectories()
        val classesDirectory = workspace.resolve("generated-java-classes").createDirectories()
        val sourceFiles = javaArtifacts.map { artifact -> writeSourceFile(sourceDirectory, artifact) }
        val compiler =
            ToolProvider.getSystemJavaCompiler()
                ?: fail("Generated Java source compilation requires a JDK, but no system Java compiler was found")
        val diagnostics = DiagnosticCollector<JavaFileObject>()

        compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8).use { fileManager ->
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, listOf(classesDirectory))
            val compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourceFiles)
            val success =
                compiler
                    .getTask(
                        null,
                        fileManager,
                        diagnostics,
                        listOf("-classpath", classpath),
                        null,
                        compilationUnits,
                    ).call()

            if (!success) {
                fail("Generated Java source compilation failed:\n${diagnostics.format()}")
            }
        }

        return GeneratedJavaCompilation(classesDirectory)
    }

    private fun writeSourceFile(
        sourceDirectory: Path,
        artifact: GeneratedArtifact,
    ): Path {
        val sourceFile = sourceDirectory.resolve(artifact.relativePath)
        sourceFile.parent.createDirectories()
        sourceFile.writeText(artifact.content)
        return sourceFile
    }

    private fun DiagnosticCollector<JavaFileObject>.format(): String =
        diagnostics.joinToString(separator = System.lineSeparator()) { diagnostic ->
            val source = diagnostic.source?.name ?: "<unknown source>"
            val position = "${diagnostic.lineNumber}:${diagnostic.columnNumber}"
            val kind = diagnostic.kind.name.lowercase(Locale.ROOT)
            val message = diagnostic.getMessage(Locale.ROOT)
            "$source:$position: $kind: $message"
        }
}

/**
 * Result of compiling generated Java source artifacts in tests.
 */
internal data class GeneratedJavaCompilation(
    val classesDirectory: Path,
) {
    fun classLoader(): URLClassLoader =
        URLClassLoader(
            arrayOf(classesDirectory.toUri().toURL()),
            Thread.currentThread().contextClassLoader,
        )
}
