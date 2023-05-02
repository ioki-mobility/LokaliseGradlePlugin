package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.DownloadStringsConfig
import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal abstract class DownloadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val apiToken: Property<String>

    @get:Input
    abstract val lokaliseOutputDir: Property<File>

    @get:Input
    abstract val arguments: ListProperty<String>

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val command = listOf(
            "${lokaliseOutputDir.get()}/lokalise",
            "file",
            "download",
            "--token",
            apiToken.get(),
            "--project-id",
            projectId.get(),
            *arguments.get().toTypedArray()
        )
        logger.log(LogLevel.INFO, "Execute the following command:\n$command")
        project.exec {
            it.commandLine(command)
        }
    }
}

internal fun TaskContainer.registerDownloadTranslationTask(
    config: DownloadStringsConfig,
    lokaliseExtensions: LokaliseExtension,
    unzipLokaliseTask: Provider<UnzipLokaliseCliTask>
): TaskProvider<DownloadTranslationsTask> = register(
    "downloadTranslationsFor${config.name.replaceFirstChar { it.titlecase() }}",
    DownloadTranslationsTask::class.java
) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.lokaliseOutputDir.set(unzipLokaliseTask.map { task -> task.destinationDir })
    it.arguments.set(config.arguments)
}