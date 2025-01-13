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
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class CheckEverythingTranslatedTaskTest {

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
            lokalise {
                apiToken.set("AWESOM3-AP1-T0KEN")
                projectId.set("AW3S0ME-PR0J3C7-1D")
                downloadStringsConfigs {
                    register("library") {
                        checkTranslationProcess = true
                    }
                    register("flavor")
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
        expectThat(result.output).contains("Check if all keys are translated for library")
        expectThat(result.output).contains("Check if all keys are translated for flavor")
    }

    @Test
    fun `running downloadTranslationsForLibrary will first run checkEverythingIsTranslatedForLibrary`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForLibrary")
            .buildAndFail()

        expectThat(result.output).contains(":checkEverythingIsTranslatedForLibrary")
        expectThat(result.task(":checkEverythingIsTranslatedForLibrary"))
            .isNotNull()
            .get { outcome }
            .isEqualTo(TaskOutcome.FAILED) // Failed because of API call, but not skipped
    }

    @Test
    fun `running downloadTranslationsForFlavor will skip checkEverythingIsTranslatedForFlavor`() {
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("downloadTranslationsForFlavor")
            .buildAndFail()

        expectThat(result.task(":checkEverythingIsTranslatedForFlavor"))
            .isNotNull()
            .get { outcome }
            .isEqualTo(TaskOutcome.SKIPPED)
    }

    @Test
    fun `running checkEverythingIsTranslatedForLibrary if project is not translated will throw`() {
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")
        buildGradle.writeText(
            """
            import com.ioki.lokalise.gradle.plugin.tasks.CheckEverythingTranslatedTask
            import com.ioki.lokalise.gradle.plugin.*
            import com.ioki.lokalise.api.models.Project as LokaliseProject
                    
            plugins {
                id("com.ioki.lokalise")
            }
                
            val fakeLokaliseApi: LokaliseProjectApi = object : LokaliseProjectApi {
                override suspend fun getProject(): LokaliseProject {
                    return ${lokaliseProject(10)}
                }
            }
            
            tasks.register<CheckEverythingTranslatedTask>("testCheckEverythingTranslatedTask") {
                lokaliseApiFactory.set({ fakeLokaliseApi })
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("testCheckEverythingTranslatedTask", "--info")
            .buildAndFail()

        expectThat(result.output.contains("Not all keys are translated"))
    }

    @Test
    fun `running checkEverythingIsTranslatedForLibrary if project is translated will succeed`() {
        val buildGradle = Paths.get(tempDir.toString(), "build.gradle.kts")
        buildGradle.writeText(
            """
            import com.ioki.lokalise.gradle.plugin.tasks.CheckEverythingTranslatedTask
            import com.ioki.lokalise.gradle.plugin.*
            import com.ioki.lokalise.api.models.Project as LokaliseProject
                    
            plugins {
                id("com.ioki.lokalise")
            }
                
            val fakeLokaliseApi: LokaliseProjectApi = object : LokaliseProjectApi {
                override suspend fun getProject(): LokaliseProject {
                    return ${lokaliseProject(100)}
            }
            
            tasks.register<CheckEverythingTranslatedTask>("testCheckEverythingTranslatedTask") {
                lokaliseApiFactory.set({ fakeLokaliseApi })
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .withProjectDir(tempDir.toFile())
            .withPluginClasspath()
            .withArguments("testCheckEverythingTranslatedTask", "--info")
            .buildAndFail()

        expectThat(result.output.contains("Not all keys are translated"))
    }
}

private fun lokaliseProject(totalProgress: Int): String = """
LokaliseProject(
    projectId = "",
    projectType = "",
    name = "",
    description = "",
    createdAt = "",
    createdAtTimestamp = 0,
    createdBy = 0,
    createdByEmail = "",
    teamId = 0,
    baseLanguageId = 0,
    baseLanguageIso = "",
    settings = LokaliseProject.Settings(
        perPlatformKeyNames = false,
        reviewing = false,
        autoToggleUnverified = false,
        offlineTranslation = false,
        keyEditing = false,
        inlineMachineTranslations = false,
        branching = false,
        segmentation = false,
        customTranslationStatuses = false,
        customTranslationStatusesAllowMultiple = false,
        contributorPreviewDownloadEnabled = false
    ),
    statistics = LokaliseProject.Statistics(
        progressTotal = $totalProgress,
        keysTotal = 0,
        team = 0,
        baseWords = 0,
        qaIssuesTotal = 0,
        qaIssues = LokaliseProject.Statistics.QaIssues(
            notReviewed = 0,
            unverified = 0,
            spellingGrammar = 0,
            inconsistentPlaceholders = 0,
            inconsistentHtml = 0,
            differentNumberOfUrls = 0,
            differentUrls = 0,
            leadingWhitespace = 0,
            trailingWhitespace = 0,
            differentNumberOfEmailAddress = 0,
            differentEmailAddress = 0,
            differentBrackets = 0,
            differentNumbers = 0,
            doubleSpace = 0,
            specialPlaceholder = 0,
            unbalancedBrackets = 0
        ),
        languages = emptyList()
    )
)
""".trimIndent()