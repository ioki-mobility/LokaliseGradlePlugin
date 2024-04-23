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
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

version = "2.2.0"
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
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            name = "SonatypeSnapshot"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
            name = "SonatypeStaging"
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin.jvmToolchain(8)

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    isRequired = hasProperty("GPG_SIGNING_REQUIRED")
    if (isRequired) useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
