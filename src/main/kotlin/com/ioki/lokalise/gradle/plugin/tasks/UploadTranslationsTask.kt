package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

internal abstract class UploadTranslationsTask : DefaultTask() {

    @Input
    lateinit var projectId: Provider<String>

    @Input
    lateinit var apiToken: Provider<String>

    @Input
    lateinit var translationFilesToUpload: Provider<ConfigurableFileTree>

    @Input
    lateinit var lokaliseCliOutputDir: Provider<File>

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val fileTree = translationFilesToUpload.get()
        val stringFilesAsString = fileTree.toList().joinToString {
            it.path.replace(fileTree.dir.absolutePath, ".")
        }
        project.exec {
            it.commandLine(
                "${lokaliseCliOutputDir.get()}/lokalise",
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