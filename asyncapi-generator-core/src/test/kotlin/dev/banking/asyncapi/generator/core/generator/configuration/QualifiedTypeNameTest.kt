package dev.banking.asyncapi.generator.core.generator.configuration

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QualifiedTypeNameTest {
    @Test
    fun `fromConfigurationValue creates qualified type name`() {
        val typeName =
            QualifiedTypeName.fromConfigurationValue(
                value = "com.example.codegen.GeneratedPayload",
                path = "modelConfig.modelAnnotation",
            )

        assertEquals("com.example.codegen.GeneratedPayload", typeName.value)
        assertEquals("GeneratedPayload", typeName.simpleName)
    }

    @Test
    fun `fromConfigurationValue rejects empty type name`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                QualifiedTypeName.fromConfigurationValue(
                    value = "",
                    path = "modelConfig.modelAnnotation",
                )
            }

        assertEquals("modelConfig.modelAnnotation cannot be empty", exception.message)
    }

    @Test
    fun `fromConfigurationValue requires fully qualified type name`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                QualifiedTypeName.fromConfigurationValue(
                    value = "GeneratedPayload",
                    path = "modelConfig.modelAnnotation",
                )
            }

        assertEquals(
            "modelConfig.modelAnnotation must be a fully qualified type name, for example com.example.GeneratedPayload",
            exception.message,
        )
    }
}
