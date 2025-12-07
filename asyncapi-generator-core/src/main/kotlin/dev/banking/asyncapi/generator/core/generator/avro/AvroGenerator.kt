package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.factory.AvroGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroEnum
import dev.banking.asyncapi.generator.core.generator.avro.model.AvroRecord
import dev.banking.asyncapi.generator.core.model.schemas.Schema
import java.io.File

class AvroGenerator(
    outputDir: File,
    packageName: String
) {
    private val factory = AvroGeneratorModelFactory(packageName)
    private val generator = AvroSchemaGenerator(outputDir)

    fun generate(schemas: Map<String, Schema>) {
           schemas.forEach { (name, schema) ->
               val item = factory.create(name, schema)
               if (item is AvroRecord || item is AvroEnum) {
                   generator.generate(item)
               }
           }
       }
}
