package dev.banking.asyncapi.generator.core.generator.kotlin

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.sourcetarget.SourceTarget

/** Kotlin sources: data classes and Kotlin Spring Kafka clients. */
internal object KotlinTarget : SourceTarget {
    override val language = SourceLanguage.KOTLIN

    override val clientSyntax = KotlinClientSyntax

    override fun modelEmitter(models: ModelGeneration.Enabled): ModelEmitter = KotlinModelEmitter(models.annotation)
}
