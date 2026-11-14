plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.asyncapi.generator)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jackson.annotations)
    implementation(libs.spring.kafka)
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
            enabled = false
        }
    }
    executions {
        register("producer") {
            generatorName = "kotlin"
            inputSpec = file("src/main/asyncapi/asyncapi.yaml")
            modelPackage = "com.example.producer.model"
            clientPackage = "com.example.producer.client"
        }
    }
}
