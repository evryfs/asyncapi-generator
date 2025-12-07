package com.tietoevry.banking.asyncapi.generator.core.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.yaml.snakeyaml.DumperOptions

class AsyncApiListSerializer : JsonSerializer<List<*>>() {
    override fun serialize(value: List<*>?, gen: JsonGenerator, serializers: SerializerProvider) {

        if (value == null) {
            gen.writeNull()
            return
        }

        val yamlGen = gen as? YAMLGenerator

        val allSimple = value.all { it is Number || it is String }

        if (yamlGen != null && allSimple) {

            val outputOptionsField = YAMLGenerator::class.java.getDeclaredField("_outputOptions")
            outputOptionsField.isAccessible = true
            val options = outputOptionsField.get(yamlGen) as DumperOptions

            val oldFlowStyle = options.defaultFlowStyle

            options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW

            try {
                gen.writeStartArray()
                value.forEach { serializers.defaultSerializeValue(it, gen) }
                gen.writeEndArray()
            } finally {
                options.defaultFlowStyle = oldFlowStyle
            }
        }

        else {
            gen.writeStartArray()
            value.forEach { serializers.defaultSerializeValue(it, gen) }
            gen.writeEndArray()
        }
    }
}
