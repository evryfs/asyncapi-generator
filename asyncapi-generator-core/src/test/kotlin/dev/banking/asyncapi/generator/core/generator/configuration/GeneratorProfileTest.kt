package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.SourceLanguage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeneratorProfileTest {
    @Test
    fun `source profile retains its source language`() {
        assertEquals(
            SourceLanguage.KOTLIN,
            GeneratorProfile.Source(SourceLanguage.KOTLIN).language,
        )
    }

    @Test
    fun `schema profile retains its schema type`() {
        assertEquals(
            SchemaType.AVRO,
            GeneratorProfile.Schema(SchemaType.AVRO).type,
        )
    }

    @Test
    fun `document profile retains its document format`() {
        assertEquals(
            DocumentFormat.YAML,
            GeneratorProfile.Document(DocumentFormat.YAML).format,
        )
    }
}
