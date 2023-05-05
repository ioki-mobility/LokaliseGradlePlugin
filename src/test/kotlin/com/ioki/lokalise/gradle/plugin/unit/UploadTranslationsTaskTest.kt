package com.ioki.lokalise.gradle.plugin.unit

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class UploadTranslationsTaskTest {

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun `setup lokalise test project dir`() {
        Paths.get(tempDir.toString(), "settings.gradle").createFile()
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")

        buildGradle.writeText(
            """
            plugins {
                id("com.ioki.lokalise")
            }
            val filesToUpload = provider {
                fileTree(rootDir) {
                    include("build.gradle.kts")
                    include("settings.gradle")
                }
            }
            lokalise {
                apiToken.set("AWESOM3-AP1-T0KEN")
                projectId.set("AW3S0ME-PR0J3C7-1D")
                translationsFilesToUpload.set(filesToUpload)
            }
        """.trimIndent()
        )
    }

    @Test
    fun `running uploadingTranslations task has successful outcome of downloadLokaliseCli`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("uploadTranslations")
            .buildAndFail()

        expectThat(result.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `running uploadTranslations task has been called but failed because of wrong token`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("uploadTranslations")
            .buildAndFail()

        expectThat(result.task(":uploadTranslations")?.outcome).isEqualTo(TaskOutcome.FAILED)
        expectThat(result.output).contains("400 Invalid `X-Api-Token`")
    }

    @Test
    fun `running uploadTranslations task contains defined files to upload but failed because of wrong token`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("uploadTranslations", "--info")
            .buildAndFail()

        val expectBuildAndSettingOrSettingAndBuild = Regex(
            "\\./build\\.gradle\\.kts,\\./settings\\.gradle" +
                    "|" +
                    "\\./settings\\.gradle,\\./build\\.gradle\\.kts"
        )
        expectThat(result.output).contains(expectBuildAndSettingOrSettingAndBuild)
        expectThat(result.output).contains("400 Invalid `X-Api-Token`")
    }

    @Test
    fun `do not throw if configuration cache is enabled`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("uploadTranslations", "--configuration-cache", "--info")
            .buildAndFail()

        expectThat(result.task(":uploadTranslations")?.outcome).isEqualTo(TaskOutcome.FAILED)
        expectThat(result.output).contains("400 Invalid `X-Api-Token`")
        expectThat(result.output.contains("Configuration cache problems found in this build")).isFalse()
    }
}