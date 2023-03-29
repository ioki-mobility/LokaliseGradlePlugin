package com.ioki.lokalise.gradle.plugin

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
import kotlin.test.assertContains
import kotlin.test.assertTrue

class LokaliseGradlePluginTest {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun moveTestProjectToTestTmpDir() {
        Paths.get(tempDir.toString(), "settings.gradle")
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")

        buildGradle.writeText(
            """
            plugins {
                id("com.ioki.lokalise")
            }
            val filesToUpload = provider {
                fileTree(rootDir) {
                    include("build.gradle.kts")
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
    fun `running downloadTranslations task has successful outcome of downloadLokalise`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslations")
            .buildAndFail()

        expectThat(result.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `running downloadTranslations task has been called but failed because of wrong token`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslations")
            .buildAndFail()

        expectThat(result.task(":downloadTranslations")?.outcome).isEqualTo(TaskOutcome.FAILED)
        expectThat(result.output).contains("400 Invalid `X-Api-Token`")
    }

    @Test
    fun `running uploadingTranslations task has successful outcome of downloadLokalise`() {
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
}