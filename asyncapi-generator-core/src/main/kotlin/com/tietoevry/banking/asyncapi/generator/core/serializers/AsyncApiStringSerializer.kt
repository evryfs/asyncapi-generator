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
            // Not YAML (e.g. JSON) – just write as a normal string
            gen.writeString(value)
            return
        }

        // First non-space char is our optional style prefix
        val trimmedStart = value.trimStart()
        val prefix = trimmedStart.firstOrNull()

        // Clean the value we actually want to emit
        val cleaned: String = when (prefix) {
            '|', '>' -> {
                val withoutMarker = trimmedStart.drop(1)
                // Drop a single immediate newline after the marker, if present
                withoutMarker
                    .removePrefix("\r\n")
                    .removePrefix("\n")
            }
            '\'', '"' -> {
                // For quoted-style prefix, drop the prefix and trim once
                trimmedStart.drop(1).trim()
            }
            else -> value // No style prefix → leave untouched
        }

        // Choose scalar style from the prefix
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
            // Safety net: if reflection breaks for some reason, fall back to normal behavior
            gen.writeString(cleaned)
        }
    }
}
