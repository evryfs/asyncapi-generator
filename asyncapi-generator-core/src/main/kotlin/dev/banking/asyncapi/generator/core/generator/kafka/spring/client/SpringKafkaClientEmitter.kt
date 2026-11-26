package dev.banking.asyncapi.generator.core.generator.kafka.spring.client

import dev.banking.asyncapi.generator.core.generator.messaging.AnalyzedChannel
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifact
import dev.banking.asyncapi.generator.core.generator.output.GeneratedArtifactKind
import dev.banking.asyncapi.generator.core.generator.output.GenerationResult
import dev.banking.asyncapi.generator.core.generator.output.TemplateRenderer
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ClientEmitter

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
    private val templates = TemplateRenderer(syntax.templateRoot, GeneratedArtifactKind.SOURCE)

    override fun emit(channels: List<AnalyzedChannel>): GenerationResult =
        GenerationResult(channels.flatMap { channel -> viewFactory.create(channel).map(::render) })

    private fun render(view: SpringKafkaClientView): GeneratedArtifact {
        val templateName =
            when (view) {
                is SpringKafkaClientView.Consumer -> "spring-kafka-consumer.mustache"
                is SpringKafkaClientView.Producer -> "spring-kafka-producer.mustache"
            }
        return templates.render(view.packageName, "${view.name}.${syntax.fileExtension}", templateName, view)
    }
}
