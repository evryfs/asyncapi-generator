package com.tietoevry.banking.asyncapi.generator.core.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import org.yaml.snakeyaml.DumperOptions

/**
 * Custom serializer for Lists that intelligently decides whether
 * to write the list in standard block-style (`- item`)
 * or inline flow-style (`[item1, item2, item3]`).
 *
 * This is used in AsyncAPI YAML output to produce compact,
 * spec-friendly arrays like:
 *
 * examples:
 *   - [1, 2, 3]
 *
 * while keeping other lists (like `required:` or `tags:`)
 * readable in block form.
 */
class AsyncApiListSerializer : JsonSerializer<List<*>>() {

    /**
     * Core serialization logic that Jackson calls whenever a List<T> is encountered.
     */
    override fun serialize(value: List<*>?, gen: JsonGenerator, serializers: SerializerProvider) {

        // Handle nulls early
        // If the value is null, just write a YAML `null` and return.
        if (value == null) {
            gen.writeNull()
            return
        }

        // Detect if we’re serializing to YAML
        // Jackson can use the same ObjectMapper for JSON, YAML, etc.
        // We only want to apply YAML-specific logic when the generator
        // is actually a YAMLGenerator.
        val yamlGen = gen as? YAMLGenerator

        // Determine if this list is "simple"
        // We only want inline flow-style for simple, flat lists like [1, 2, 3] or ["a", "b"].
        // More complex structures (maps, nested arrays, objects)
        // should still use standard block style for readability.
        val allSimple = value.all { it is Number || it is String }

        // Apply flow-style temporarily for simple lists
        if (yamlGen != null && allSimple) {

            // 4.1 Access SnakeYAML's internal DumperOptions via reflection.
            // Jackson doesn't expose this, so we reflect into the private field
            // `_outputOptions` of YAMLGenerator to tweak formatting options.
            val outputOptionsField = YAMLGenerator::class.java.getDeclaredField("_outputOptions")
            outputOptionsField.isAccessible = true
            val options = outputOptionsField.get(yamlGen) as DumperOptions

            // 4.2 Remember the current global flow style.
            // (By default it's usually `BLOCK`, meaning lists are written with `- item`.)
            val oldFlowStyle = options.defaultFlowStyle

            // 4.3 Temporarily set the style to `FLOW`, so lists render like `[1, 2, 3]`.
            options.defaultFlowStyle = DumperOptions.FlowStyle.FLOW

            try {
                // 4.4 Now we can serialize the list normally.
                // Under the hood, SnakeYAML will see that the default style is FLOW
                // and produce an inline list instead of a multi-line one.
                gen.writeStartArray()
                value.forEach { serializers.defaultSerializeValue(it, gen) }
                gen.writeEndArray()
            } finally {
                // 4.5 Always restore the previous flow style afterward
                // so we don’t accidentally affect other lists being serialized later.
                options.defaultFlowStyle = oldFlowStyle
            }
        }

        // Fallback for all other cases
        else {
            // If not YAML, or the list is complex (contains objects, maps, etc.),
            // we use standard Jackson array emission, which writes YAML like:
            //
            // items:
            //   - first
            //   - second
            //
            gen.writeStartArray()
            value.forEach { serializers.defaultSerializeValue(it, gen) }
            gen.writeEndArray()
        }
    }
}
