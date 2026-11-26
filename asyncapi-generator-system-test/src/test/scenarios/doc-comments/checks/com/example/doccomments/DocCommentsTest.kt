package com.example.doccomments

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Descriptions reach the generated documentation as the contract writes them, Markdown markers and quotes included. */
class DocCommentsTest {
    private val descriptions =
        listOf("> Quoted from the policy.", "| Column | Meaning |", "\"Quoted\" wording stays.", "|pipe and >arrow")

    @Test
    fun `Kotlin and Java doc comments keep descriptions as written`() {
        for (source in listOf("com/example/doccomments/kotlin/Notice.kt", "com/example/doccomments/java/Notice.java")) {
            val text = generatedSource(source).readText()
            descriptions.forEach { description -> assertTrue(description in text, "$source lacks '$description'") }
        }
    }

    @Test
    fun `Avro docs keep descriptions as written`() {
        val schema = ObjectMapper().readTree(javaClass.getResource("/com/example/doccomments/avro/Notice.avsc"))
        val fieldDocs = schema["fields"].associate { it["name"].asText() to it["doc"].asText() }

        assertEquals("> Quoted from the policy. | Column | Meaning |", schema["doc"].asText())
        assertEquals("\"Quoted\" wording stays.", fieldDocs["body"])
        assertEquals("|pipe and >arrow", fieldDocs["marker"])
    }

    /** The generated source file at [path], wherever the build tool writes generated sources. */
    private fun generatedSource(path: String): File =
        File(System.getProperty("user.dir"))
            .walk()
            .first { it.isFile && it.invariantSeparatorsPath.endsWith(path) }
}
