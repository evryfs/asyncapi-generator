package dev.banking.asyncapi.generator.core.generator.kotlin.kafka.spring

import com.github.mustachejava.DefaultMustacheFactory
import dev.banking.asyncapi.generator.core.generator.kotlin.model.GeneratorItem
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import java.io.StringWriter

class KotlinSpringKafkaProducerGenerator {
    private val mustacheFactory = DefaultMustacheFactory("kotlin")

    fun render(model: GeneratorItem.KafkaProducerClass): GeneratedArtifact {
        val template = mustacheFactory.compile("spring-kafka-producer.mustache")
        val writer = StringWriter()
        template.execute(writer, model).flush()

        return GeneratedArtifact(
            relativePath = GeneratedArtifactPaths.fromNamespace(model.packageName, "${model.name}.kt"),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
