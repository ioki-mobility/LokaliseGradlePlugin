package com.ioki.lokalise.gradle.plugin.instrumentation

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.DefaultAsserter.fail

class LokaliseGradlePluginPublishingTest {
    @TempDir
    lateinit var testTmpPath: Path

    private lateinit var settingsGradle: Path

    private lateinit var buildGradle: Path

    @BeforeEach
    fun `setup lokalise test project dir`() {
        settingsGradle = Paths.get(testTmpPath.toString(), "settings.gradle.kts")
        buildGradle = Paths.get(testTmpPath.toString(), "build.gradle.kts")

        settingsGradle.writeText(
            """
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    maven(url = "https://jitpack.io") // For transitive dependencies
                    mavenLocal()
                }
            }
            """.trimIndent()
        )
        buildGradle.writeText(
            """
            plugins {
                id("com.ioki.lokalise")
            }
            lokalise {
                apiToken.set("AWESOM3-AP1-T0KEN")
                projectId.set("AW3S0ME-PR0J3C7-1D")
            }
        """.trimIndent()
        )
    }

    @Test
    fun `consuming of plugin publication via mavenLocal works`() {
        val newBuildFile = buildGradle.readText().replace(
            oldValue = """id("com.ioki.lokalise")""",
            newValue = """id("com.ioki.lokalise") version "1.1.0""""
        )
        buildGradle.writeText(newBuildFile)

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withArguments("tasks")
            .build()

        expectThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun `consuming of plugin publication via jitpack works`() {
        var testVersion = System.getenv("IOKI_LOKALISE_PLUGIN_TEST_VERSION")
            ?: fail(
                "Please provide plugin version from jitpack" +
                    " via environment variable 'IOKI_LOKALISE_PLUGIN_TEST_VERSION'"
            )
        val isSemverVersion = Regex("[0-9]+\\.[0-9]+\\.[0-9]+").matches(testVersion)
        if (!isSemverVersion) {
            testVersion += "-SNAPSHOT"
        }
        val newBuildFile = buildGradle.readText().replace(
            oldValue = """id("com.ioki.lokalise")""",
            newValue = """id("com.ioki.lokalise") version "$testVersion""""
        )
        buildGradle.writeText(newBuildFile)
        val newSettingsFile = settingsGradle.readText().replace(
            oldValue = """gradlePluginPortal()""",
            newValue =
            """
                gradlePluginPortal() 
                maven(url = "https://jitpack.io")
                resolutionStrategy {
                    eachPlugin {
                        if (requested.id.id == "com.ioki.lokalise") {
                            useModule(
                                   "com.github.ioki-mobility.LokaliseGradlePlugin:lokalise:${'$'}{requested.version}"
                            )
                        }
                    }
                }
            """
        )
        settingsGradle.writeText(newSettingsFile)

        val result: BuildResult = GradleRunner.create()
            .withProjectDir(testTmpPath.toFile())
            .withArguments("tasks")
            .build()

        expectThat(result.output).contains("BUILD SUCCESSFUL")
    }
}