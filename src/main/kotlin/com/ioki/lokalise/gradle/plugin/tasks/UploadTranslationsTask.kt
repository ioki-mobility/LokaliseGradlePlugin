package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import javax.inject.Inject

internal abstract class UploadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val apiToken: Property<String>

    @get:Input
    abstract val translationFilesToUpload: Property<ConfigurableFileTree>

    @get:Input
    abstract val lokaliseCliFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val fileTree = translationFilesToUpload.get()
        val stringFilesAsString = fileTree.toList().joinToString(separator = ",") {
            it.path.replace(fileTree.dir.absolutePath, ".")
        }
        execOperations.exec {
            it.commandLine(
                lokaliseCliFile.get(),
                "file",
                "upload",
                "--token",
                apiToken.get(),
                "--project-id",
                projectId.get(),
                "--file",
                stringFilesAsString,
                "--replace-modified",
                "--cleanup-mode",
                "--include-path",
                "--distinguish-by-file",
                "--lang-iso", "en_BZ",
                "--poll"
            )
        }
    }
}

internal fun TaskContainer.registerUploadTranslationTask(
    lokaliseExtensions: LokaliseExtension,
    lokaliseCliFile: Provider<RegularFileProperty>,
): TaskProvider<UploadTranslationsTask> = register("uploadTranslations", UploadTranslationsTask::class.java) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.lokaliseCliFile.set(lokaliseCliFile.map { it.get() })
    it.translationFilesToUpload.set(lokaliseExtensions.translationsFilesToUpload)
}