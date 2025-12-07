package com.tietoevry.banking.asyncapi.generator.core.generator.avro

import com.github.mustachejava.DefaultMustacheFactory
import com.tietoevry.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import com.tietoevry.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import com.tietoevry.banking.asyncapi.generator.core.generator.avro.model.AvroSchema
import com.tietoevry.banking.asyncapi.generator.core.generator.avro.model.AvroUnion
import com.tietoevry.banking.asyncapi.generator.core.generator.util.FileUtil
import java.io.File
import java.io.StringWriter

class AvroSchemaGenerator(
    private val outputDir: File
) {
    private val mustacheFactory = DefaultMustacheFactory("avro")

    fun generate(schemaItem: AvroSchema) {
        when (schemaItem) {
            is AvroRecord -> generateRecord(schemaItem)
            is AvroUnion -> generateUnion(schemaItem)
            is AvroEnum -> generateEnum(schemaItem)
        }
    }

    private fun generateRecord(record: AvroRecord) {
        val template = mustacheFactory.compile("avro.mustache")
        val writer = StringWriter()
        template.execute(writer, record).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, record.namespace)
        val outputFile = File(packageDir, "${record.name}.avsc")

        outputFile.writeText(writer.toString())
    }

    private fun generateUnion(union: AvroUnion) {
        val template = mustacheFactory.compile("avro-union.mustache")
        val writer = StringWriter()
        template.execute(writer, union).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, union.namespace)
        val outputFile = File(packageDir, "${union.name}.avsc")

        outputFile.writeText(writer.toString())
    }

    private fun generateEnum(enumModel: AvroEnum) {
        val template = mustacheFactory.compile("avro-enum.mustache")
        val writer = StringWriter()
        template.execute(writer, enumModel).flush()

        val packageDir = FileUtil.packageDirectory(outputDir, enumModel.namespace)
        val outputFile = File(packageDir, "${enumModel.name}.avsc")

        outputFile.writeText(writer.toString())
    }
}

