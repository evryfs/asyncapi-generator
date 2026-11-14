package dev.banking.asyncapi.generator.core.generator.java.kafka.spring

import dev.banking.asyncapi.generator.core.generator.util.SourceMustacheFactory
import dev.banking.asyncapi.generator.core.generator.java.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

internal class JavaSpringKafkaProducerGenerator {
    private val mustacheFactory = SourceMustacheFactory("java")

    fun render(model: GeneratorItem.KafkaProducerClass): GeneratedArtifact {
        val template = mustacheFactory.compile("spring-kafka-producer.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.java"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
