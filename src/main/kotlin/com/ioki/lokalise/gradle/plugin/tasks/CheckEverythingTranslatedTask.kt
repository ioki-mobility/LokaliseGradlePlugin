package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.DownloadStringsConfig
import com.ioki.lokalise.gradle.plugin.LokaliseApiFactory
import com.ioki.lokalise.gradle.plugin.LokaliseProjectApi
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider

abstract class CheckEverythingTranslatedTask : DefaultTask() {

    @get:Input
    abstract val lokaliseApiFactory: Property<() -> LokaliseProjectApi>

    @TaskAction
    fun f() {
        val project = runBlocking { lokaliseApiFactory.get().invoke().getProject() }
        if (project.statistics.progressTotal != 100) {
            throw GradleException("Not all keys are translated")
        }
    }
}

internal fun TaskContainer.registerCheckEverythingTranslatedTask(
    lokaliseApiFactory: LokaliseApiFactory,
    config: DownloadStringsConfig,
): TaskProvider<CheckEverythingTranslatedTask> = register(
    "checkEverythingIsTranslatedFor${config.name.replaceFirstChar { it.titlecase() }}",
    CheckEverythingTranslatedTask::class.java
) {
    it.lokaliseApiFactory.set(lokaliseApiFactory::createProjectApi)
    it.onlyIf { config.checkTranslationProcess.getOrElse(false) }
}