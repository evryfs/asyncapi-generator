package dev.banking.asyncapi.generator.core.generator.configuration

import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelTypeResolverTest {
    @Test
    fun `resolve applies source generator defaults`() {
        assertEquals(
            ModelType.KOTLIN_DATA_CLASS,
            ModelTypeResolver.resolve(GeneratorName.KOTLIN, configuredModelType = null),
        )
        assertEquals(
            ModelType.JAVA_CLASS,
            ModelTypeResolver.resolve(GeneratorName.JAVA, configuredModelType = null),
        )
    }

    @Test
    fun `resolve accepts native payload models for both source generators`() {
        listOf(GeneratorName.KOTLIN, GeneratorName.JAVA).forEach { generatorName ->
            assertEquals(
                ModelType.AVRO_SPECIFIC_RECORD,
                ModelTypeResolver.resolve(generatorName, ModelType.AVRO_SPECIFIC_RECORD),
            )
            assertEquals(
                ModelType.PROTOBUF_MESSAGE,
                ModelTypeResolver.resolve(generatorName, ModelType.PROTOBUF_MESSAGE),
            )
        }
    }

    @Test
    fun `resolve rejects model type from another source generator`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                ModelTypeResolver.resolve(
                    generatorName = GeneratorName.KOTLIN,
                    configuredModelType = ModelType.JAVA_RECORD,
                )
            }

        assertEquals(
            "modelConfig.modelType 'java-record' is not supported when generatorName is kotlin. " +
                "Supported values: kotlin-data-class, avro-specific-record, protobuf-message",
            exception.message,
        )
    }
}
