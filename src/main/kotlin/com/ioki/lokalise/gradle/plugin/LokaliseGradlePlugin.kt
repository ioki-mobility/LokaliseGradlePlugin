package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val lokaliseExtensions = project.extensions.createLokaliseExtension()

        val apiFactory = LokaliseUploadApiFactory(
            apiTokenProvider = lokaliseExtensions.apiToken,
            projectIdProvider = lokaliseExtensions.projectId
        )

        project.tasks.registerUploadTranslationTask(
            lokaliseUploadApiFactory = apiFactory,
            lokaliseExtensions = lokaliseExtensions,
        )

        val downloadTranslationsForAll = project.tasks.register("downloadTranslationsForAll")
        lokaliseExtensions.downloadStringsConfigs.all {
            val customDownloadTask = project.tasks.registerDownloadTranslationTask(
                config = it,
                lokaliseExtensions = lokaliseExtensions,
            )
            downloadTranslationsForAll.configure { allTask -> allTask.dependsOn(customDownloadTask) }
        }
    }
}

