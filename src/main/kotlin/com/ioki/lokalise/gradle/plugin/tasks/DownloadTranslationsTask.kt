package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        project.exec {
            it.commandLine(
                "${lokaliseOutputDir.get()}/lokalise",
                "file",
                "download",
                "--token",
                apiToken.get(),
                "--project-id",
                projectId.get(),
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
                "--indentation",
                "4sp",
                "--replace-breaks=false"
            )
        }
    }
}

internal fun TaskContainer.registerDownloadTranslationTask(
    lokaliseExtensions: LokaliseExtension,
    unzipLokaliseTask: Provider<UnzipLokaliseCliTask>
): TaskProvider<DownloadTranslationsTask> = register("downloadTranslations", DownloadTranslationsTask::class.java) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.lokaliseOutputDir.set(unzipLokaliseTask.map { task -> task.destinationDir })
}