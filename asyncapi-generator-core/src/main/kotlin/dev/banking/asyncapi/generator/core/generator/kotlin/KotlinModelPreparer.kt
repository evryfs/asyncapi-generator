package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.kotlin.factory.KotlinGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.loader.HeaderSchemaCollector
import dev.banking.asyncapi.generator.core.model.asyncapi.AsyncApiDocument

/**
 * Prepares Kotlin model items from analyzed generator input.
 *
 * Expected behavior is covered by:
 * - `KotlinModelPreparerTest`
 */
class KotlinModelPreparer {
    fun prepare(
        input: GenerationInput,
        packageName: String,
        annotation: QualifiedTypeName? = null,
    ): List<GeneratorItem> {
        val factory =
            KotlinGeneratorModelFactory(
                packageName = packageName,
                context = input.schemaContext,
                polymorphicRelationships = input.polymorphicRelationships,
                annotation = annotation,
            )

        return input.schemas.mapNotNull { (name, schema) ->
            factory.create(name, schema)
        }
    }

    fun prepareHeaders(
        input: GenerationInput,
        asyncApiDocument: AsyncApiDocument,
        packageName: String,
    ): List<GeneratorItem> {
        val headerSchemas = HeaderSchemaCollector.collect(asyncApiDocument)
        if (headerSchemas.isEmpty()) return emptyList()

        val factory =
            KotlinGeneratorModelFactory(
                packageName = packageName,
                context = input.schemaContextWith(headerSchemas),
                polymorphicRelationships = input.polymorphicRelationships,
                annotation = null,
            )

        return headerSchemas.mapNotNull { (name, schema) ->
            factory.create(name, schema)
        }
    }
}
