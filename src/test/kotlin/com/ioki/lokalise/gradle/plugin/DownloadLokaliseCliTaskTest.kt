package com.ioki.lokalise.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.writeText

class DownloadLokaliseCliTaskTest {
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun moveTestProjectToTestTmpDir() {
        Paths.get(tempDir.toString(), "settings.gradle")
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")

        buildGradle.writeText("""
            plugins {
                id("com.ioki.lokalise")
            }
        """.trimIndent())
    }

    @Test
    fun `running task has successful outcome`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadLokaliseCli")
            .build()

        assert(result.task(":downloadLokaliseCli")?.outcome == TaskOutcome.SUCCESS)
    }

    @Test
    fun `running task will download the cli at correct dir`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("unzipLokaliseCli")
            .build()

        expectThat(result.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        expectThat(tempDir.resolve("build/lokalise/cli").exists()).isTrue()
    }

    @Test
    fun `running task will download the cli only once`() {
        val runner = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadLokaliseCli")

        val result1 = runner.build()

        expectThat(result1.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val result2 = runner.build()

        expectThat(result2.task(":downloadLokaliseCli")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
    }
}