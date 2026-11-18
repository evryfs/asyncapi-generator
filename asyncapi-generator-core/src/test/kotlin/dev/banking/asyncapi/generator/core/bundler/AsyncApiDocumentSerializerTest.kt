package dev.banking.asyncapi.generator.core.bundler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.TestResources
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument
import dev.banking.asyncapi.generator.core.model.components.Component
import dev.banking.asyncapi.generator.core.model.info.Info
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import dev.banking.asyncapi.generator.core.model.schemas.SchemaInterface
import dev.banking.asyncapi.generator.core.parser.node.ParserNodeFactory
import dev.banking.asyncapi.generator.core.source.SourceReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncApiDocumentSerializerTest {
    @Test
    fun `write then read preserves YAML scalar values and types`(
        @TempDir tempDir: Path,
    ) {
        val context = AsyncApiContext()
        val document =
            AsyncApiDocument(
                asyncapi = "3.0.0",
                info =
                    Info(
                        title = "Value preservation",
                        version = "123",
                        description = "First line\nSecond line",
                        extensions =
                            linkedMapOf(
                                "x-leading-pipe" to "|leading-indicator",
                                "x-leading-gt" to ">leading-indicator",
                                "x-leading-trailing-whitespace" to "  whitespace preserved  ",
                                "x-quoted-both-sides" to "\"quoted\"",
                                "x-yaml-word" to "on",
                                "x-yaml-date" to "2026-08-03",
                                "x-yaml-timestamp" to "2026-08-03T12:34:56Z",
                                "x-string-list" to
                                    listOf("true", "123", "null", "|field", ">field", "'field", "\"field"),
                            ),
                    ),
                components =
                    Component(
                        schemas =
                            linkedMapOf(
                                "SampleSchema" to
                                    SchemaInterface.SchemaInline(
                                        Schema(
                                            type = listOf("string", "null"),
                                        ),
                                    ),
                            ),
                    ),
            )

        val yamlFile = tempDir.resolve("roundtrip.yaml").toFile()
        yamlFile.writeText(AsyncApiDocumentSerializer.toYaml(document))

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

        val root = ParserNodeFactory.root(SourceReader.read(yamlFile), context)
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

        val schemaType =
            rootObject
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

    @Test
    fun `YAML and JSON serialization emit each items representation exactly once`() {
        val schemas =
            linkedMapOf(
                "tuple" to
                    Schema(
                        tupleItems =
                            listOf(
                                SchemaInterface.SchemaInline(Schema(type = "string")),
                                SchemaInterface.SchemaInline(Schema(type = "integer")),
                            ),
                    ),
                "single" to
                    Schema(
                        items = SchemaInterface.SchemaInline(Schema(type = "string")),
                    ),
                "false" to
                    Schema(
                        items = SchemaInterface.BooleanSchema(false),
                    ),
            )

        val yaml = AsyncApiDocumentSerializer.toYaml(schemas)
        val json = AsyncApiDocumentSerializer.toJson(schemas)
        val yamlSchemas = ObjectMapper(YAMLFactory()).readTree(yaml)
        val jsonSchemas = ObjectMapper().readTree(json)

        assertEquals(2, yamlSchemas.path("tuple").path("items").size())
        assertEquals(
            "string",
            yamlSchemas
                .path("tuple")
                .path("items")
                .path(0)
                .path("type")
                .textValue(),
        )
        assertEquals(
            "integer",
            yamlSchemas
                .path("tuple")
                .path("items")
                .path(1)
                .path("type")
                .textValue(),
        )
        assertEquals(
            "string",
            yamlSchemas
                .path("single")
                .path("items")
                .path("type")
                .textValue(),
        )
        assertEquals(false, yamlSchemas.path("false").path("items").booleanValue())
        assertEquals(
            setOf("items"),
            yamlSchemas
                .path("tuple")
                .fieldNames()
                .asSequence()
                .toSet(),
        )
        assertEquals(3, Regex("(?m)^\\s*items:").findAll(yaml).count())
        assertFalse(yaml.contains("tupleItems"))
        assertFalse(yaml.contains("serializedItems"))

        assertEquals(2, jsonSchemas.path("tuple").path("items").size())
        assertEquals(
            "string",
            jsonSchemas
                .path("tuple")
                .path("items")
                .path(0)
                .path("type")
                .textValue(),
        )
        assertEquals(
            "integer",
            jsonSchemas
                .path("tuple")
                .path("items")
                .path(1)
                .path("type")
                .textValue(),
        )
        assertEquals(
            "string",
            jsonSchemas
                .path("single")
                .path("items")
                .path("type")
                .textValue(),
        )
        assertEquals(false, jsonSchemas.path("false").path("items").booleanValue())
        assertEquals(
            setOf("items"),
            jsonSchemas
                .path("tuple")
                .fieldNames()
                .asSequence()
                .toSet(),
        )
        assertEquals(3, Regex("\"items\"\\s*:").findAll(json).count())
        assertFalse(json.contains("tupleItems"))
        assertFalse(json.contains("serializedItems"))
    }
}
