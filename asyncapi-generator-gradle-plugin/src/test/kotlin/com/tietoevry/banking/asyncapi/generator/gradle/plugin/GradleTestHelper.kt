package dev.banking.asyncapi.generator.gradle.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

object GradleTestHelper {

    fun resourceFile(path: String): File {
        val url = Thread.currentThread().contextClassLoader.getResource(path)
            ?: throw IllegalArgumentException("Test resource not found: $path")
        return File(url.toURI())
    }

    fun writeBuildScript(dir: File, content: String): File =
        File(dir, "build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(content.trimIndent())
        }

    fun runGradle(dir: File, vararg args: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments(*args)
            .forwardOutput()
            .build()

    fun runGradleAndFail(dir: File, vararg args: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(dir)
            .withPluginClasspath()
            .withArguments(*args)
            .forwardOutput()
            .buildAndFail()

    fun copyGeneratedOutputToProjectBuild(sourceDir: File) {
        val targetDir = File("build/asyncapi-generator-gradle-debug")
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()
        if (sourceDir.exists()) sourceDir.copyRecursively(targetDir, overwrite = true)
    }
}

