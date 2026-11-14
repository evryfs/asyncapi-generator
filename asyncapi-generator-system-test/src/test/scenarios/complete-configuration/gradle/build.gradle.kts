plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.asyncapi.generator)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jackson.annotations)
    implementation(libs.spring.kafka)
    implementation(libs.avro)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.jackson.module.kotlin)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

tasks.withType<Test>() {
    useJUnitPlatform()
}

asyncApiGenerator {
    clientConfig {
        clientType = "spring-kafka"
        clientContract = "interface"
        producer {
            enabled = true
        }
        consumer {
            enabled = true
        }
        topicParameterProperties.put("environment", "kafka.environment")
        validationAnnotations {
            clientContract = "org.springframework.validation.annotation.Validated"
            payloadParameter = "jakarta.validation.Valid"
        }
    }
    executions {
        register("kotlin-client") {
            generatorName = "kotlin"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/kotlin")
            outputFile = layout.projectDirectory.file("bundles/asyncapi.yaml")
            modelPackage = "com.example.complete.kotlin.model"
            clientPackage = "com.example.complete.kotlin.client"
            modelConfig {
                modelAnnotation = "com.example.codegen.GeneratedPayload"
            }
        }
        register("java-records") {
            generatorName = "java"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/java")
            modelPackage = "com.example.complete.java.model"
            modelConfig {
                modelType = "java-record"
                modelAnnotation = "com.example.codegen.GeneratedPayload"
            }
        }
        register("native-avro") {
            generatorName = "java"
            inputSpec = file("src/main/asyncapi/native-avro.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/avro")
            modelPackage = "com.example.complete.avro"
            modelConfig {
                modelType = "avro-specific-record"
            }
        }
        register("native-protobuf") {
            generatorName = "kotlin"
            inputSpec = file("src/main/asyncapi/native-protobuf.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/protobuf")
            modelPackage = "com.example.complete.protobuf"
            modelConfig {
                modelType = "protobuf-message"
            }
        }
        register("avro-schema") {
            generatorName = "avro-schema"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/schemas/avro")
            schemaPackage = "com.example.complete.schema.avro"
        }
        register("protobuf-schema") {
            generatorName = "protobuf-schema"
            inputSpec = file("src/main/asyncapi/native-protobuf.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/schemas/protobuf")
            schemaPackage = "com.example.complete.schema.protobuf"
        }
        register("json-schema") {
            generatorName = "json-schema"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputDirectory = layout.projectDirectory.dir("generated/schemas/json")
            schemaPackage = "com.example.complete.schema.json"
        }
        register("asyncapi-yaml") {
            generatorName = "asyncapi-yaml"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputFile = layout.projectDirectory.file("documents/asyncapi.yaml")
        }
        register("asyncapi-json") {
            generatorName = "asyncapi-json"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            outputFile = layout.projectDirectory.file("documents/asyncapi.json")
        }
    }
}
