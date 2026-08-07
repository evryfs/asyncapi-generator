package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.factory.AvroGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.Schema

/**
 * Renders parsed schemas into Avro schema artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `AvroGeneratorTest`
 * - `AvroSchemaApprovalTest`
 */
class AvroGenerator(
    packageName: String,
) {
    private val factory = AvroGeneratorModelFactory(packageName)
    private val generator = AvroSchemaGenerator()

    fun render(schemas: Map<String, Schema>): GenerationResult =
        GenerationResult(
            schemas.mapNotNull { (name, schema) ->
                factory.create(name, schema)?.let(generator::render)
            },
        )
}
