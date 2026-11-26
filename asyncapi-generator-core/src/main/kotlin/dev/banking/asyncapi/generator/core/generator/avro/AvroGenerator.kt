package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.factory.AvroGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.input.GenerationInput
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinition
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelDefinitionFactory
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidGeneratedAvroSchema
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException

/**
 * Renders the models of a contract as Avro schema artifacts, in the order of their names.
 *
 * @param packageName the Avro namespace of the generated schemas
 */
internal class AvroGenerator(
    private val packageName: String,
) {
    private val generator = AvroSchemaGenerator()

    fun render(input: GenerationInput): GenerationResult {
        val models = ModelDefinitionFactory(input, packageName, AvroNaming).create()
        val factory = AvroGeneratorModelFactory(packageName, models)
        return GenerationResult(
            models
                .sortedBy(ModelDefinition::name)
                .mapNotNull { model -> factory.create(model)?.let(generator::render) }
                .also(::verifyParses),
        )
    }

    /**
     * Parses the generated schemas with Apache Avro, each after the schemas it refers to, so that a
     * reference to a schema that is not generated fails here and not in the consumer's build.
     * Schemas that refer to each other in a cycle cannot be parsed one by one and are skipped.
     */
    private fun verifyParses(artifacts: List<GeneratedArtifact>) {
        val fullNames = artifacts.associateBy { it.relativePath.removeSuffix(".avsc").replace('/', '.') }
        val dependencies =
            artifacts.associateWith { artifact ->
                fullNames.filter { (name, other) -> other != artifact && "\"$name\"" in artifact.content }.keys
            }
        val parser = Schema.Parser()
        val parsed = mutableSetOf<String>()
        var pending = fullNames.entries.toList()
        while (pending.isNotEmpty()) {
            val ready = pending.filter { (_, artifact) -> parsed.containsAll(dependencies.getValue(artifact)) }
            if (ready.isEmpty()) return
            ready.forEach { (name, artifact) ->
                try {
                    parser.parse(artifact.content)
                } catch (exception: SchemaParseException) {
                    throw InvalidGeneratedAvroSchema(artifact.relativePath, exception.message.orEmpty())
                }
                parsed += name
            }
            pending = pending - ready.toSet()
        }
    }
}
