package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.TemplateRenderer

/**
 * Renders Avro records and enums as `.avsc` schema artifacts.
 */
internal class AvroSchemaGenerator {
    private val templates = TemplateRenderer("avro", GeneratedArtifactKind.SCHEMA)

    fun render(schemaItem: AvroSchema): GeneratedArtifact =
        when (schemaItem) {
            is AvroRecord ->
                templates.render(schemaItem.namespace, "${schemaItem.name}.avsc", "avro.mustache", schemaItem)
            is AvroEnum ->
                templates.render(schemaItem.namespace, "${schemaItem.name}.avsc", "avro-enum.mustache", schemaItem)
        }
}
