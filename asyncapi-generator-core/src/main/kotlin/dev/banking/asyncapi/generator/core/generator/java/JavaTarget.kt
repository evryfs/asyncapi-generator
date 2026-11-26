package dev.banking.asyncapi.generator.core.generator.java

import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.SourceLanguage
import dev.banking.asyncapi.generator.core.generator.sourcemodel.ModelEmitter
import dev.banking.asyncapi.generator.core.generator.sourcetarget.SourceTarget

/** Java sources: classes or records and Java Spring Kafka clients. */
internal object JavaTarget : SourceTarget {
    override val language = SourceLanguage.JAVA

    override val clientSyntax = JavaClientSyntax

    override fun modelEmitter(models: ModelGeneration.Enabled): ModelEmitter =
        JavaModelEmitter(JavaModelType.of(models.modelType), models.annotation)
}
