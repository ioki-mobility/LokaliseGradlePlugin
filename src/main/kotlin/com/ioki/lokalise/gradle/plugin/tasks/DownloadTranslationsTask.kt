package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

internal abstract class DownloadTranslationsTask : DefaultTask() {

    @Input
    lateinit var projectId: Provider<String>

    @Input
    lateinit var apiToken: Provider<String>

    @Input
    lateinit var lokaliseOutputDir: Provider<File>

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