package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.api.models.DownloadFilesRequest
import com.ioki.lokalise.gradle.plugin.DownloadStringsConfig
import com.ioki.lokalise.gradle.plugin.LokaliseApiFactory
import com.ioki.lokalise.gradle.plugin.LokaliseDownloadApi
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

internal abstract class DownloadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val lokaliseApiFactory: Property<() -> LokaliseDownloadApi>

    @get:Input
    abstract val requestBody: Property<DownloadFilesRequest>

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @get:Input
    @get:Optional
    abstract val downloadAsync: Property<Boolean>

    @TaskAction
    fun f() {
        logger.log(LogLevel.INFO, "Execute downloading files with the following request body:\n${requestBody.get()}")

        val downloadedFile = runBlocking {
            val lokaliseApi = lokaliseApiFactory.get()()
            if (downloadAsync.get()) {
                lokaliseApi.downloadFilesAsync(requestBody.get())
            } else {
                lokaliseApi.downloadFiles(requestBody.get())
            }
        }
        val bundleUrl = downloadedFile.bundleUrl

        val outputZipFile = projectLayout.buildDirectory.file("lokalise/translations.zip")
            .get()
            .asFile
            .apply { parentFile.mkdirs() }
        URL(bundleUrl).openStream().use {
            Files.copy(it, outputZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        fileSystemOperations.copy { copy ->
            copy.from(archiveOperations.zipTree(outputZipFile))
            copy.into(projectLayout.projectDirectory.asFile)
            copy.eachFile {
                it.relativePath = RelativePath(true, *it.relativePath.segments.drop(1).toTypedArray())
            }
        }
    }
}

internal fun TaskContainer.registerDownloadTranslationTask(
    lokaliseApiFactory: LokaliseApiFactory,
    downloadAsync: Property<Boolean>,
    config: DownloadStringsConfig,
): TaskProvider<DownloadTranslationsTask> = register(
    "downloadTranslationsFor${config.name.replaceFirstChar { it.titlecase() }}",
    DownloadTranslationsTask::class.java
) {
    it.lokaliseApiFactory.set(lokaliseApiFactory::createDownloadApi)
    it.requestBody.set(it.project.provider { config.toRequestBody() })
    it.downloadAsync.set(downloadAsync)
    it.group = "Lokalise"
    it.description = "Download translations from Lokalise for ${config.name}"
}