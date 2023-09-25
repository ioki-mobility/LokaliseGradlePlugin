package com.ioki.lokalise.gradle.plugin.unit

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

class DownloadAllTranslationsTaskTest {
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
                    register("main") {
                        params(
                            "--format" to "xml",
                            "--filter-langs" to listOf("en","de","de_CH","fr_CH","es","it","nl","ca","ar"),
                        )
                    }
                    register("spanishOnly") {
                        params(
                            "--format" to "xml",
                            "--filter-langs" to listOf("es"),
                        )
                    }
                }
            }
        """.trimIndent()
        )
    }

    @Test
    fun `running downloadTranslationsForAll task will execute ForMain and ForSpanishOnly tasks too`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForAll", "--continue")
            .buildAndFail()

        expectThat(result.tasks.map { it.path }).contains(
            ":downloadTranslationsForMain",
            ":downloadTranslationsForSpanishOnly"
        )
    }
}