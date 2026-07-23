package dev.banking.asyncapi.generator.core.fixtures

import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.fail

/**
 * Compiles generated Kotlin source artifacts and their generated Java dependencies inside tests.
 *
 * Use this fixture when generated Kotlin output builds on generated Java APIs, as the official
 * Protobuf Kotlin DSL does. Compilation runs against the current test classpath so the fixture
 * verifies both source sets and their required runtime libraries together.
 */
internal class GeneratedKotlinCompiler(
    private val classpath: String = System.getProperty("java.class.path"),
    private val javaCompiler: GeneratedJavaCompiler = GeneratedJavaCompiler(classpath),
) {
    fun compile(
        artifacts: Iterable<GeneratedArtifact>,
        workspace: Path,
    ): GeneratedKotlinCompilation {
        val kotlinArtifacts = artifacts.filter { artifact -> artifact.kind == GeneratedArtifactKind.SOURCE }
        require(kotlinArtifacts.isNotEmpty()) {
            "Expected at least one Kotlin source artifact to compile"
        }

        val javaCompilation =
            artifacts
                .filter { artifact -> artifact.kind == GeneratedArtifactKind.JAVA_SOURCE }
                .takeIf(List<GeneratedArtifact>::isNotEmpty)
                ?.let { javaArtifacts -> javaCompiler.compile(javaArtifacts, workspace) }
        val sourceDirectory = workspace.resolve("generated-kotlin-sources").createDirectories()
        val classesDirectory = workspace.resolve("generated-kotlin-classes").createDirectories()
        val sourceFiles = kotlinArtifacts.map { artifact -> writeSourceFile(sourceDirectory, artifact) }
        val compilerOutput = ByteArrayOutputStream()
        val compilationClasspath =
            listOfNotNull(
                classpath,
                javaCompilation?.classesDirectory?.toString(),
            ).joinToString(File.pathSeparator)
        val exitCode =
            PrintStream(compilerOutput, true, StandardCharsets.UTF_8).use { output ->
                K2JVMCompiler().exec(
                    output,
                    "-no-stdlib",
                    "-no-reflect",
                    "-classpath",
                    compilationClasspath,
                    "-d",
                    classesDirectory.toString(),
                    *sourceFiles.map(Path::toString).toTypedArray(),
                )
            }

        if (exitCode != ExitCode.OK) {
            fail(
                "Generated Kotlin source compilation failed with ${exitCode.name}:\n" +
                    compilerOutput.toString(StandardCharsets.UTF_8),
            )
        }

        return GeneratedKotlinCompilation(
            classesDirectory = classesDirectory,
            javaClassesDirectory = javaCompilation?.classesDirectory,
        )
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
}

/**
 * Result of compiling generated Kotlin and supporting Java sources in tests.
 */
internal data class GeneratedKotlinCompilation(
    val classesDirectory: Path,
    val javaClassesDirectory: Path?,
)
