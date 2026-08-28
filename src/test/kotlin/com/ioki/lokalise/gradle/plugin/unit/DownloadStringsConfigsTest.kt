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
                    register("flavor") {
                        requestBody.set(
                            DownloadFilesRequest(
                                format = "xml",
                                filterLangs = listOf("en","de","de_CH","fr_CH","es","it","nl","ca","ar"),
                                exportEmptyAs = "skip",
                                includeDescription = false,
                                exportSort = "first_added",
                                directoryPrefix = ".",
                                filterFilenames = listOf("./src/${"$"}{findProperty("flavor")}/res/values-%LANG_ISO%/strings.xml"),
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