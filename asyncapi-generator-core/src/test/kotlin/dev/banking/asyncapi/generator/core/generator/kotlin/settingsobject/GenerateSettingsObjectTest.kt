package dev.banking.asyncapi.generator.core.generator.kotlin.settingsobject

import dev.banking.asyncapi.generator.core.generator.AbstractKotlinGeneratorClass
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class GenerateSettingsObjectTest : AbstractKotlinGeneratorClass() {

    @Test
    fun generate_asyncapi_settings_object_type_SettingsObjectType_dataClass() {
        val generated = generateElement(
            yaml = File("src/test/resources/generator/asyncapi_settings_object_type.yaml"),
            generated = "SettingsObjectType.kt",
            modelPackage = "dev.banking.asyncapi.generator.core.model.generated.settingsobject",
        )
        val dataClass = extractElement(generated)
        val expected = """
        data class SettingsObjectType(

            val mode: Mode,

            @field:Min(0L)
            @field:Max(10L)
            val retryLimit: Int? = null,

            val extraSettings: Map<String, Any>? = null
        ) {
        }
    """.trimIndent()
        assertEquals(expected, dataClass)
    }
}
