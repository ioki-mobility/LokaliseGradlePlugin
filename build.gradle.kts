plugins {
    alias(libs.plugins.kotlin)
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
}

gradlePlugin {
    plugins {
        register("com.ioki.lokalise") {
            id = "com.ioki.lokalise"
            implementationClass = "com.ioki.lokalise.gradle.plugin.LokaliseGradlePlugin"
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

version = "2.0.0"
group = "com.ioki.lokalise"
publishing {
    publications {
        register("pluginMaven", MavenPublication::class.java) {
            artifactId = "lokalise-gradle-plugin"
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
}

tasks.test {
    useJUnitPlatform()
}

kotlin.jvmToolchain(8)

signing {
    val signingKey = System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("GPG_SIGNING_PASSWORD")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}
