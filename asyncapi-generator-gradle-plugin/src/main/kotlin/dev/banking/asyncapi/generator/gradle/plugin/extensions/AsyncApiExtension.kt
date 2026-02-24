package dev.banking.asyncapi.generator.gradle.plugin.extensions

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class AsyncApiExtension @Inject constructor(objects: ObjectFactory) {
    val inputFile: RegularFileProperty = objects.fileProperty()
    val outputFile: RegularFileProperty = objects.fileProperty()
    val outputDir: DirectoryProperty = objects.directoryProperty()

    val modelPackage: Property<String> = objects.property(String::class.java)
    val clientPackage: Property<String> = objects.property(String::class.java)
    val schemaPackage: Property<String> = objects.property(String::class.java)

    val generatorName: Property<String> = objects.property(String::class.java)

    val configOptions: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)
}
