import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

val asyncApiGeneratorVersion: String by project

plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("com.gradle.plugin-publish") version "2.1.1"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "dev.banking.asyncapi.generator"
version = asyncApiGeneratorVersion

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation("dev.banking.asyncapi.generator:asyncapi-generator-core:$asyncApiGeneratorVersion") {
        isChanging = true
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.20")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation(gradleTestKit())

}

gradlePlugin {
    website.set("https://github.com/evryfs/asyncapi-generator")
    vcsUrl.set("https://github.com/evryfs/asyncapi-generator.git")
    plugins.register("asyncapiGenerator") {
        id = "dev.banking.asyncapi.generator"
        implementationClass = "dev.banking.asyncapi.generator.gradle.plugin.AsyncApiPlugin"
        displayName = "AsyncAPI Generator Plugin"
        description = "Generates AsyncAPI clients and models for Spring Kafka"
        tags.set(listOf("asyncapi", "kotlin", "java", "avro", "kafka", "spring", "generator"))
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("AsyncAPI Generator Gradle Plugin")
            description.set("A flexible AsyncAPI code generator for Kotlin and Java Spring Kafka clients.")
            url.set("https://github.com/evryfs/asyncapi-generator")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("@bascunansalvador")
                    name.set("Salvador Bascunan")
                    email.set("bascunan.salvador@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/evryfs/asyncapi-generator.git")
                developerConnection.set("scm:git:ssh://github.com/evryfs/asyncapi-generator.git")
                url.set("https://github.com/evryfs/asyncapi-generator")
            }
        }
    }
}

signing {
    val signingKey = System.getenv("OSSRH_GPG_SECRET_KEY")
    val signingPassword = System.getenv("OSSRH_GPG_PASSPHRASE")
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
    isRequired = System.getenv("CI") != null
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_TOKEN"))
        }
    }
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
