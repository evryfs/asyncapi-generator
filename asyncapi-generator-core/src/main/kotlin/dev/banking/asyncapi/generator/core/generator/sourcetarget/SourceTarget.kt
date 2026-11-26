package dev.banking.asyncapi.generator.core.generator.sourcetarget

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.kafka.spring.client.SpringKafkaClientSyntax
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelEmitter

/**
 * What generating sources in one language needs: the emitter of its payload models and the syntax
 * of its Spring Kafka clients. Each language package implements one; adding a language means adding
 * a [SourceLanguage] entry and its target.
 */
internal interface SourceTarget {
    /** The language this target writes. */
    val language: SourceLanguage

    /** The syntax of the Spring Kafka clients in this language. */
    val clientSyntax: SpringKafkaClientSyntax

    /** The emitter of payload models with the model type and annotation of [models]. */
    fun modelEmitter(models: ModelGeneration.Enabled): ModelEmitter
}
