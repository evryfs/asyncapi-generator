package com.example.javadochtml

import java.io.File
import java.io.StringWriter
import javax.tools.ToolProvider
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

/** Descriptions with `<` and `&` reach the generated Javadoc as HTML that `javadoc` accepts. */
class JavadocHtmlTest {
    @Test
    fun `javadoc accepts the generated Java sources`() {
        val sources = generatedSources()
        val output = StringWriter()
        val tool = ToolProvider.getSystemDocumentationTool()
        val fileManager = tool.getStandardFileManager(null, null, null)
        val task =
            tool.getTask(
                output,
                fileManager,
                null,
                null,
                listOf(
                    "-quiet",
                    "-Xdoclint:all,-missing",
                    "-d",
                    createTempDirectory("javadoc").toString(),
                    "-classpath",
                    System.getProperty("java.class.path"),
                ),
                fileManager.getJavaFileObjectsFromFiles(sources),
            )

        assertTrue(task.call(), "javadoc rejected the generated sources:\n$output")
    }

    @Test
    fun `the Javadoc text reads as the contract wrote it`() {
        val order = generatedSources().single { it.name == "Order.java" }.readText()

        assertTrue("Totals must be &lt; 100 &amp; above 0. Written escaped as &lt;b> too." in order, order)
        assertTrue("Salt &amp; pepper &lt; 5 grams." in order, order)
    }

    /** The generated Java sources, wherever the build tool writes them. */
    private fun generatedSources(): List<File> =
        File(System.getProperty("user.dir"))
            .walk()
            .filter { it.isFile && it.extension == "java" && "generated" in it.invariantSeparatorsPath }
            .filter { "com/example/javadochtml/" in it.invariantSeparatorsPath }
            .toList()
            .also { assertTrue(it.isNotEmpty(), "no generated Java sources found") }
}
