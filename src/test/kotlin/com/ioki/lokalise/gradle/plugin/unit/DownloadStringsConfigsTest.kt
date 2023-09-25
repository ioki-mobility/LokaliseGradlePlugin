package com.ioki.lokalise.gradle.plugin.unit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

class DownloadStringsConfigsTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun `setup lokalise test project dir`() {
        Paths.get(tempDir.toString(), "settings.gradle")
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")

        buildGradle.writeText(
            """
            plugins {
                id("com.ioki.lokalise")
            }
            
            lokalise {
                apiToken.set("AWESOM3-AP1-T0KEN")
                projectId.set("AW3S0ME-PR0J3C7-1D")
                downloadStringsConfigs {
                    register("library") {
                        params(
                            "--format" to "xml",
                            "--filter-langs" to listOf("en","de","de_CH","fr_CH","es","it","nl","ca","ar"),
                            "--export-empty-as" to "skip",
                            "--include-description" to false,
                            "--export-sort" to "first_added",
                            "--directory-prefix" to ".",
                            "--filter-filenames" to listOf("./src/main/res/values-%LANG_ISO%/strings.xml"),
                            "--indentation" to "4sp",
                            "--replace-breaks" to "false"
                        )   
                    }
                    register("flavor") {
                        params(
                            "--format" to "xml",
                            "--filter-langs" to listOf("en","de","de_CH","fr_CH","es","it","nl","ca","ar"),
                            "--export-empty-as" to "skip",
                            "--include-description" to false,
                            "--export-sort" to "first_added",
                            "--directory-prefix" to ".",
                            "--filter-filenames" to listOf("./src/${"$"}{findProperty("flavor")}/res/values-%LANG_ISO%/strings.xml"),
                            "--indentation" to "4sp",
                            "--replace-breaks" to "false"
                        )   
                    }
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `running downloadTranslationsForFlavor task is created and can run but fails because of wrong credentials`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForFlavor", "-Pflavor=hamburg", "--info")
            .buildAndFail()

        expectThat(result.task(":downloadTranslationsForFlavor")).isNotNull()
        expectThat(result.task(":downloadTranslationsForFlavor")?.outcome).isEqualTo(TaskOutcome.FAILED)
    }

    @Test
    fun `running downloadTranslationsForFlavor task should set filter filenames correctly`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForLibrary", "--info")
            .buildAndFail()

        expectThat(result.task(":downloadTranslationsForLibrary")).isNotNull()
        expectThat(result.task(":downloadTranslationsForLibrary")?.outcome).isEqualTo(TaskOutcome.FAILED)
    }
}