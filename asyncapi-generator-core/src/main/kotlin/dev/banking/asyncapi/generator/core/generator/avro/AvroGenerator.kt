package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.factory.AvroGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import java.io.File

/**
 * Renders parsed schemas into Avro schema artifacts before writing them.
 *
 * Expected behavior is covered by:
 * - `AvroGeneratorTest`
 * - `AvroSchemaApprovalTest`
 */
class AvroGenerator(
    private val outputDir: File,
    packageName: String,
) {
    private val factory = AvroGeneratorModelFactory(packageName)
    private val generator = AvroSchemaGenerator(outputDir)

    fun generate(schemas: Map<String, Schema>) {
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(render(schemas))
    }

    fun render(schemas: Map<String, Schema>): GenerationResult =
        GenerationResult(
            schemas.mapNotNull { (name, schema) ->
                val item = factory.create(name, schema)
                if (item is AvroRecord || item is AvroEnum) {
                    generator.render(item)
                } else {
                    null
                }
            },
        )
}
