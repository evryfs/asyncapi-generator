package dev.banking.asyncapi.generator.core.generator.avro

import dev.banking.asyncapi.generator.core.generator.avro.factory.AvroGeneratorModelFactory
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.model.schemas.Schema as AsyncApiSchema
import org.apache.avro.Schema
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.model.exceptions.AsyncApiGeneratorException.InvalidGeneratedAvroSchema
import org.apache.avro.SchemaParseException

/**
 * Renders parsed schemas into Avro schema artifacts before writing them.
 */
internal class AvroGenerator(
    packageName: String,
) {
    private val factory = AvroGeneratorModelFactory(packageName)
    private val generator = AvroSchemaGenerator()

    fun render(schemas: Map<String, AsyncApiSchema>): GenerationResult =
        GenerationResult(
            schemas.entries
                .sortedBy(Map.Entry<String, AsyncApiSchema>::key)
                .mapNotNull { (name, schema) ->
                    factory.create(name, schema)?.let(generator::render)
                }.also(::verifyParses),
        )

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
