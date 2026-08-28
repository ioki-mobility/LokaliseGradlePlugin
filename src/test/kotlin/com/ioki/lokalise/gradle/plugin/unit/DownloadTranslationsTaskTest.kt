package com.ioki.lokalise.gradle.plugin.unit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.doesNotContain
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class DownloadTranslationsTaskTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun `setup lokalise test project dir`() {
        Paths.get(tempDir.toString(), "settings.gradle").createFile()
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")

        buildGradle.writeText(
            """
            import com.ioki.lokalise.api.models.DownloadFilesRequest

            plugins {
                id("com.ioki.lokalise")
            }
            lokalise {
                apiToken.set("AWESOM3-AP1-T0KEN")
                projectId.set("AW3S0ME-PR0J3C7-1D")
                downloadStringsConfigs {
                    register("library") {
                        requestBody.set(
                            DownloadFilesRequest(
                                format = "xml",
                                filterLangs = listOf("en","de","de_CH","fr_CH","es","it","nl","ca","ar"),
                                exportEmptyAs = "skip",
                                includeDescription = false,
                                exportSort = "first_added",
                                directoryPrefix = ".",
                                filterFilenames = listOf("./src/main/res/values-%LANG_ISO%/strings.xml"),
                                indentation = "4sp",
                                replaceBreaks = false,
                            )
                        )
                    }
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `tasks has group and description`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("tasks")
            .build()

        expectThat(result.output).contains("Lokalise")
        expectThat(result.output).contains("Download translations from Lokalise for library")
    }

    @Test
    fun `running downloadTranslationsForLibrary task has been called but failed because of wrong token`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForLibrary")
            .buildAndFail()

        expectThat(result.task(":downloadTranslationsForLibrary")?.outcome).isEqualTo(TaskOutcome.FAILED)
        expectThat(result.output).contains("Invalid `X-Api-Token`")
    }

    @Test
    fun `do not throw if configuration cache is enabled`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForLibrary", "--configuration-cache", "--info")
            .buildAndFail()

        expectThat(result.task(":downloadTranslationsForLibrary")?.outcome).isEqualTo(TaskOutcome.FAILED)
        expectThat(result.output).contains("Invalid `X-Api-Token")
        expectThat(result.output.contains("Configuration cache problems found in this build")).isFalse()
    }
}