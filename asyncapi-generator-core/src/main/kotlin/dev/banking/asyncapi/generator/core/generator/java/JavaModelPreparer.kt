package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.QualifiedTypeName
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.java.factory.JavaGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem

/**
 * Prepares Java model items from analyzed generator input.
 */
class JavaModelPreparer {
    fun prepare(
        input: GenerationInput,
        packageName: String,
        annotation: QualifiedTypeName? = null,
    ): List<GeneratorItem> {
        val factory =
            JavaGeneratorModelFactory(
                packageName = packageName,
                context = input.schemaContext,
                polymorphicRelationships = input.polymorphicRelationships,
                annotation = annotation,
            )

        return input.schemas.mapNotNull { (name, schema) ->
            factory.create(name, schema)
        }
    }
}
