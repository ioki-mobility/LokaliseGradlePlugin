package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.DownloadStringsConfig
import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import javax.inject.Inject

internal abstract class DownloadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val apiToken: Property<String>

    @get:InputFile
    abstract val lokaliseCliFile: RegularFileProperty

    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val command = listOf(
            lokaliseCliFile.get(),
            "file",
            "download",
            "--token",
            apiToken.get(),
            "--project-id",
            projectId.get(),
            *arguments.get().toTypedArray()
        )
        logger.log(LogLevel.INFO, "Execute the following command:\n$command")
        execOperations.exec {
            it.commandLine(command)
        }
    }
}

internal fun TaskContainer.registerDownloadTranslationTask(
    config: DownloadStringsConfig,
    lokaliseExtensions: LokaliseExtension,
    lokaliseCliFile: Provider<RegularFileProperty>,
): TaskProvider<DownloadTranslationsTask> = register(
    "downloadTranslationsFor${config.name.replaceFirstChar { it.titlecase() }}",
    DownloadTranslationsTask::class.java
) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.lokaliseCliFile.set(lokaliseCliFile.map { it.get() })
    it.arguments.set(config.arguments)
}