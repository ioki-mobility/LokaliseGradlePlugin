@file:OptIn(ExperimentalEncodingApi::class)

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    `java-gradle-plugin`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.lokaliseApi)
    implementation(libs.kotlinCoroutines)
    testImplementation(libs.test.kotlinTest)
    testImplementation(libs.test.strikt)
    testImplementation(libs.test.kotlinCoroutinesTest)
}

gradlePlugin {
    plugins {
        register("com.ioki.lokalise") {
            id = "com.ioki.lokalise"
            implementationClass = "com.ioki.lokalise.gradle.plugin.LokaliseGradlePlugin"
            displayName = "LokaliseGradlePlugin"
            description = "A Gradle plugin that can up- and download strings from lokalise"
        }
    }
}

java {
    withSourcesJar()
}

val dokkaJar = tasks.register<Jar>("dokkaJar") {
    dependsOn(tasks.dokkaGenerate)
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val base64EncodedBearerToken = Base64.encode(
    "${System.getenv("SONATYPE_USER")}:${System.getenv("SONATYPE_PASSWORD")}".toByteArray(),
)

version = "2.3.0-SNAPSHOT"
group = "com.ioki.lokalise"
publishing {
    publications {
        register("pluginMaven", MavenPublication::class.java) {
            artifact(dokkaJar)
            artifactId = "lokalise-gradle-plugin"
        }
        withType<MavenPublication>().configureEach {
            pom {
                name.set("LokaliseGradlePlugin")
                description.set("A Gradle plugin that can up- and download strings from lokalise")
                url.set("https://github.com/ioki-mobility/LokaliseGradlePlugin")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                organization {
                    name.set("ioki")
                    url.set("https://ioki.com")
                }
                developers {
                    developer {
                        name.set("Stefan 'StefMa' M.")
                        email.set("StefMaDev@outlook.com")
                        url.set("https://StefMa.guru")
                        organization.set("ioki")
                        organizationUrl.set("https://ioki.com")
                    }
                }
                scm {
                    url.set("https://github.com/ioki-mobility/LokaliseGradlePlugin")
                    connection.set("scm:git:git://github.com/ioki-mobility/LokaliseGradlePlugin.git")
                    developerConnection.set("scm:git:ssh://git@github.com:ioki-mobility/LokaliseGradlePlugin.git")
                }
            }
        }
    }
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            name = "SonatypeSnapshot"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/") {
            name = "SonatypeStaging"
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer $base64EncodedBearerToken"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin.jvmToolchain(17)

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    isRequired = hasProperty("GPG_SIGNING_REQUIRED")
    if (isRequired) useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

tasks.register<Exec>("moveOssrhStagingToCentralPortal") {
    group = "publishing"
    description = "Runs after publishAllPublicationsToSonatypeStagingRepository to move the artifacts to the central portal"

    shouldRunAfter("publishAllPublicationsToSonatypeStagingRepository")

    commandLine = listOf(
        "curl",
        "-f",
        "-X", "POST",
        "-H", "Authorization: Bearer $base64EncodedBearerToken",
        "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/com.ioki",
    )
}
