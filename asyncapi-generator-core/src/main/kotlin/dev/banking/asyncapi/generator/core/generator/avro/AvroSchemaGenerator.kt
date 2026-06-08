package dev.banking.asyncapi.generator.core.generator.avro

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroUnion
import dev.banking.asyncapi.generator.core.generator.output.FileSystemGeneratedArtifactWriter
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import java.io.File
import java.io.StringWriter

/**
 * Renders Avro schema model items into schema artifacts.
 *
 * Expected behavior is covered by:
 * - `AvroSchemaGeneratorTest`
 * - `AvroSchemaApprovalTest`
 */
class AvroSchemaGenerator(
    private val outputDir: File,
) {
    private val mustacheFactory = DefaultMustacheFactory("avro")

    fun generate(schemaItem: AvroSchema) {
        val artifact = render(schemaItem)
        FileSystemGeneratedArtifactWriter(outputDir, outputDir)
            .write(GenerationResult.of(artifact))
    }

    fun render(schemaItem: AvroSchema): GeneratedArtifact =
        when (schemaItem) {
            is AvroRecord -> renderRecord(schemaItem)
            is AvroUnion -> renderUnion(schemaItem)
            is AvroEnum -> renderEnum(schemaItem)
        }

    private fun renderRecord(record: AvroRecord): GeneratedArtifact {
        val template = mustacheFactory.compile("avro.mustache")
        val writer = StringWriter()
        template.execute(writer, record).flush()

        return schemaArtifact(record.namespace, record.name, writer.toString())
    }

    private fun renderUnion(union: AvroUnion): GeneratedArtifact {
        val template = mustacheFactory.compile("avro-union.mustache")
        val writer = StringWriter()
        template.execute(writer, union).flush()

        return schemaArtifact(union.namespace, union.name, writer.toString())
    }

    private fun renderEnum(enumModel: AvroEnum): GeneratedArtifact {
        val template = mustacheFactory.compile("avro-enum.mustache")
        val writer = StringWriter()
        template.execute(writer, enumModel).flush()

        return schemaArtifact(enumModel.namespace, enumModel.name, writer.toString())
    }

    private fun schemaArtifact(
        namespace: String,
        name: String,
        content: String,
    ): GeneratedArtifact =
        GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(namespace, "$name.avsc"),
            content = content,
            kind = GeneratedArtifactKind.SCHEMA,
        )
}
