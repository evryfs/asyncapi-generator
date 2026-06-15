package dev.banking.asyncapi.generator.core.generator

import dev.banking.asyncapi.generator.core.context.AsyncApiContext
import dev.banking.asyncapi.generator.core.fixtures.BundlerFixtures
import dev.banking.asyncapi.generator.core.generator.configuration.ClientGeneration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.GeneratorOutputConfiguration
import dev.banking.asyncapi.generator.core.generator.configuration.JavaModelType
import dev.banking.asyncapi.generator.core.generator.configuration.ModelGeneration
import dev.banking.asyncapi.generator.core.generator.model.GeneratorName
import java.io.File

abstract class AbstractJavaGeneratorClass {
    protected val asyncApiContext = AsyncApiContext()
    private val bundlerFixtures = BundlerFixtures(asyncApiContext)
    protected val generator = AsyncApiGenerator()

    protected fun generateElement(
        yaml: File,
        codegenOutputDirectory: File = File("target/generated-sources/asyncapi"),
        resourceOutputDirectory: File = File("target/generated-resources/asyncapi"),
        generated: String? = null,
        modelPackage: String,
        clientPackage: String? = null,
        schemaPackage: String? = null,
        generateModels: Boolean = true,
        generateSpringKafkaClient: Boolean = false,
        generateQuarkusKafkaClient: Boolean = false,
        modelAnnotation: String? = null,
        javaModelType: JavaModelType = JavaModelType.CLASS,
    ): String {
        val bundled = bundlerFixtures.bundledDocument(yaml)
        val effectiveClientPackage = clientPackage ?: modelPackage
        val generatorConfiguration =
            GeneratorConfiguration(
                language = GeneratorName.JAVA,
                output =
                    GeneratorOutputConfiguration(
                        sourceOutputDirectory = codegenOutputDirectory,
                        resourceOutputDirectory = resourceOutputDirectory,
                    ),
                models =
                    if (generateModels) {
                        ModelGeneration.Enabled(
                            packageName = modelPackage,
                            annotation = modelAnnotation,
                            javaModelType = javaModelType,
                        )
                    } else {
                        ModelGeneration.Disabled
                    },
                clients =
                    buildList {
                        if (generateSpringKafkaClient) {
                            add(
                                ClientGeneration.SpringKafka(
                                    packageName = effectiveClientPackage,
                                    modelPackageName = modelPackage,
                                ),
                            )
                        }
                        if (generateQuarkusKafkaClient) {
                            add(
                                ClientGeneration.QuarkusKafka(
                                    packageName = effectiveClientPackage,
                                    modelPackageName = modelPackage,
                                ),
                            )
                        }
                    },
            )
        generator.generate(
            asyncApiDocument = bundled,
            generatorConfiguration = generatorConfiguration,
        )

        if (generated != null) {
            val modelPath = modelPackage.replace('.', '/')
            val output =
                codegenOutputDirectory
                    .resolve(modelPath)
                    .resolve(generated)
            return output.readText()
        }
        return ""
    }

    protected fun extractImports(source: String): String =
        source
            .lineSequence()
            .filter { it.startsWith("import ") }
            .sorted()
            .joinToString("\n")
            .trimEnd()

    protected fun extractClassBody(source: String): String {
        val classStart =
            source
                .indexOf("public class")
                .let { if (it == -1) source.indexOf("public enum") else it }
                .let { if (it == -1) source.indexOf("public interface") else it }

        if (classStart == -1) {
            val packageEnd = source.indexOf("package ")
            if (packageEnd != -1) {
                return source
                    .substring(source.indexOf(';', packageEnd) + 1)
                    .trim()
                    .trimIndent()
            }
            return source
                .trim()
                .trimIndent()
        }
        return source
            .substring(classStart)
            .trim()
            .trimIndent()
    }
}
