package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.DownloadStringsConfig
import com.ioki.lokalise.gradle.plugin.LokaliseDownloadApi
import com.ioki.lokalise.gradle.plugin.LokaliseDownloadApiFactory
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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
    abstract val params: MapProperty<String, Any>

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @TaskAction
    fun f() {
        logger.log(LogLevel.INFO, "Execute downloading files with the following params:\n${params.get()}")

        val format = params.get()["format"]
        val newParams = params.get().toMutableMap().apply { remove("format") }

        val downloadedFile = runBlocking {
            lokaliseApiFactory.get().invoke().downloadFiles(
                format = format.toString(),
                params = newParams,
            )
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
    lokaliseDownloadApiFactory: LokaliseDownloadApiFactory,
    config: DownloadStringsConfig,
): TaskProvider<DownloadTranslationsTask> = register(
    "downloadTranslationsFor${config.name.replaceFirstChar { it.titlecase() }}",
    DownloadTranslationsTask::class.java
) {
    it.lokaliseApiFactory.set(lokaliseDownloadApiFactory::create)
    it.params.set(config.params)
}