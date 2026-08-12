package dev.banking.asyncapi.generator.core.registry

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.components.ComponentInterface
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncApiRegistryTest {

    @Test
    fun `read creates parser root through document reader stage`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/yaml/source-map.yaml")
        val root = AsyncApiRegistry.read(file, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals("3.0.0", root.expectObject().optional("asyncapi")?.expect<String>())
        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(2, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(5, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
    }

    @Test
    fun `read supports json input`() {
        val context = AsyncApiContext()
        val file = TestResources.file("reader/json/source-map.json")
        val root = AsyncApiRegistry.read(file, context)
        assertEquals("source_map.root", root.name)
        assertEquals("source_map.root", root.path)
        assertEquals("3.0.0", root.expectObject().optional("asyncapi")?.expect<String>())
        assertEquals(1, context.sourceRepository.getLine("source_map.root"))
        assertEquals(3, context.sourceRepository.getLine("source_map.root.info"))
        assertEquals(4, context.sourceRepository.getLine("source_map.root.info.title"))
        assertEquals(6, context.sourceRepository.getLine("source_map.root.info.tags[0]"))
    }

    @Test
    fun `write then read preserves YAML scalar values and types`(
        @TempDir tempDir: Path,
    ) {
        val context = AsyncApiContext()
        val document = AsyncApiDocument(
            asyncapi = "3.0.0",
            info = Info(
                title = "Value preservation",
                version = "123",
                description = "First line\nSecond line",
                extensions = linkedMapOf(
                    "x-leading-pipe" to "|leading-indicator",
                    "x-leading-gt" to ">leading-indicator",
                    "x-leading-trailing-whitespace" to "  whitespace preserved  ",
                    "x-quoted-both-sides" to "\"quoted\"",
                    "x-yaml-word" to "on",
                    "x-yaml-date" to "2026-08-03",
                    "x-yaml-timestamp" to "2026-08-03T12:34:56Z",
                    "x-string-list" to listOf("true", "123", "null", "|field", ">field", "'field", "\"field"),
                ),
            ),
            components = ComponentInterface.ComponentInline(
                Component(
                    schemas = linkedMapOf(
                        "SampleSchema" to SchemaInterface.SchemaInline(
                            Schema(
                                type = listOf("string", "null"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val yamlFile = tempDir.resolve("roundtrip.yaml").toFile()
        AsyncApiRegistry.writeYaml(yamlFile, document)

        val yamlText = yamlFile.readText()
        assertTrue(
            yamlText.contains("description: |"),
        )
        assertTrue(yamlText.contains("asyncapi: 3.0.0"))
        assertTrue(yamlText.contains("title: Value preservation"))
        assertTrue(yamlText.contains("version: \"123\""))
        assertTrue(yamlText.contains("x-yaml-word: \"on\""))
        assertTrue(yamlText.contains("x-yaml-date: \"2026-08-03\""))
        assertTrue(yamlText.contains("x-yaml-timestamp: \"2026-08-03T12:34:56Z\""))

        val root = AsyncApiRegistry.read(yamlFile, context)
        val rootObject = root.expectObject()
        val info = rootObject.required("info").expectObject()

        assertEquals("123", info.required("version").expect<String>())
        assertEquals("First line\nSecond line", info.required("description").expect<String>())
        assertEquals("  whitespace preserved  ", info.required("x-leading-trailing-whitespace").expect<String>())
        assertEquals("\"quoted\"", info.required("x-quoted-both-sides").expect<String>())
        assertEquals("|leading-indicator", info.required("x-leading-pipe").expect<String>())
        assertEquals(">leading-indicator", info.required("x-leading-gt").expect<String>())
        assertEquals("on", info.required("x-yaml-word").expect<String>())
        assertEquals("2026-08-03", info.required("x-yaml-date").expect<String>())
        assertEquals("2026-08-03T12:34:56Z", info.required("x-yaml-timestamp").expect<String>())

        val schemaType = rootObject
            .required("components")
            .expectObject()
            .required("schemas")
            .expectObject()
            .required("SampleSchema")
            .expectObject()
            .required("type")
            .expect<List<String>>()

        assertEquals(listOf("string", "null"), schemaType)

        val representativeValues = info.required("x-string-list").expect<List<String>>()
        assertEquals(listOf("true", "123", "null", "|field", ">field", "'field", "\"field"), representativeValues)
    }
}
