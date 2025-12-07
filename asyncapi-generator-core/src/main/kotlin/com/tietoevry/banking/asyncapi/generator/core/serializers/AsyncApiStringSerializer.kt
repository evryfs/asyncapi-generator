package com.tietoevry.banking.asyncapi.generator.core.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.yaml.snakeyaml.DumperOptions

class AsyncApiStringSerializer : JsonSerializer<String>() {

    companion object {
        private val WRITE_SCALAR = YAMLGenerator::class.java.getDeclaredMethod(
            "_writeScalar",
            String::class.java,
            String::class.java,
            DumperOptions.ScalarStyle::class.java
        ).apply { isAccessible = true }

        private val VERIFY_VALUE_WRITE = YAMLGenerator::class.java.getDeclaredMethod(
            "_verifyValueWrite",
            String::class.java
        ).apply { isAccessible = true }
    }

    override fun serialize(value: String?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value == null) {
            gen.writeNull()
            return
        }

        val yamlGen = gen as? YAMLGenerator
        if (yamlGen == null) {
            gen.writeString(value)
            return
        }

        val trimmedStart = value.trimStart()
        val prefix = trimmedStart.firstOrNull()

        val cleaned: String = when (prefix) {
            '|', '>' -> {
                val withoutMarker = trimmedStart.drop(1)
                withoutMarker
                    .removePrefix("\r\n")
                    .removePrefix("\n")
            }
            '\'', '"' -> {
                trimmedStart.drop(1).trim()
            }
            else -> value // No style prefix → leave untouched
        }

        val style = when (prefix) {
            '>' -> DumperOptions.ScalarStyle.FOLDED
            '|' -> DumperOptions.ScalarStyle.LITERAL
            '\'' -> DumperOptions.ScalarStyle.SINGLE_QUOTED
            '"' -> DumperOptions.ScalarStyle.DOUBLE_QUOTED
            else -> DumperOptions.ScalarStyle.PLAIN
        }

        try {
            VERIFY_VALUE_WRITE.invoke(yamlGen, "write string value")
            WRITE_SCALAR.invoke(yamlGen, cleaned, "string", style)
        } catch (_: Exception) {
            gen.writeString(cleaned)
        }
    }
}
