package com.ioki.lokalise.gradle.plugin.unit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
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
                        arguments = listOf(
                            "--format",
                            "xml",
                            "--filter-langs",
                            "en,de,de_CH,fr_CH,es,it,nl,ca,ar",
                            "--export-empty-as",
                            "skip",
                            "--include-description=false",
                            "--export-sort",
                            "first_added",
                            "--directory-prefix=.",
                            "--filter-filenames", 
                            "./src/main/res/values-%LANG_ISO%/strings.xml",
                            "--indentation",
                            "4sp",
                            "--replace-breaks=false"
                        )   
                    }
                    register("flavor") {
                        arguments = listOf(
                            "--format",
                            "xml",
                            "--filter-langs",
                            "en,de,de_CH,fr_CH,es,it,nl,ca,ar",
                            "--export-empty-as",
                            "skip",
                            "--include-description=false",
                            "--export-sort",
                            "first_added",
                            "--directory-prefix=.",
                            "--filter-filenames", 
                            "./src/${"$"}{findProperty("flavor")}/res/values-%LANG_ISO%/strings.xml",
                            "--indentation",
                            "4sp",
                            "--replace-breaks=false"
                        )   
                    }
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `running downloadTranslationsForFlavor task has successful outcome of downloadLokaliseCli`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForFlavor", "-Pflavor=hamburg", "--info")
            .buildAndFail()

        expectThat(result.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `running downloadTranslationsForFlavor task should set filter filenames correctly`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForFlavor", "-Pflavor=hamburg", "--info")
            .buildAndFail()

        expectThat(result.output).contains("./src/hamburg/res/values-%LANG_ISO%/strings.xml")
    }
}