package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.ir.ClientEmitter
import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactPaths
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.util.SourceMustacheFactory
import java.io.StringWriter

/**
 * Renders the Spring Kafka clients of channels with the templates of one source language.
 *
 * @param syntax How the source language writes the client.
 * @param viewFactory Builds the client views of a channel.
 */
internal class SpringKafkaClientEmitter(
    private val syntax: SpringKafkaClientSyntax,
    private val viewFactory: SpringKafkaClientViewFactory,
) : ClientEmitter {
    private val mustacheFactory = SourceMustacheFactory(syntax.templateRoot)

    override fun emit(channels: List<AnalyzedChannel>): GenerationResult =
        GenerationResult(channels.flatMap { channel -> viewFactory.create(channel).map(::render) })

    private fun render(view: SpringKafkaClientView): GeneratedArtifact {
        val templateName =
            when (view) {
                is SpringKafkaClientView.Consumer -> "spring-kafka-consumer.mustache"
                is SpringKafkaClientView.Producer -> "spring-kafka-producer.mustache"
            }
        val writer = StringWriter()
        mustacheFactory.compile(templateName).execute(writer, view).flush()
        return GeneratedArtifact(
            relativePath =
                GeneratedArtifactPaths.fromNamespace(
                    view.packageName,
                    "${view.name}.${syntax.fileExtension}",
                ),
            content = writer.toString(),
            kind = GeneratedArtifactKind.SOURCE,
        )
    }
}
