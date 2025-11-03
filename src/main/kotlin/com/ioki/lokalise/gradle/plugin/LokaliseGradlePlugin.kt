package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.tasks.registerCheckEverythingTranslatedTask
import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val lokaliseExtensions = project.extensions.createLokaliseExtension()

        val apiFactory = LokaliseApiFactory(
            apiTokenProvider = lokaliseExtensions.apiToken,
            projectIdProvider = lokaliseExtensions.projectId
        )

        project.tasks.registerUploadTranslationTask(
            lokaliseApiFactory = apiFactory,
            lokaliseExtensions = lokaliseExtensions,
        )

        val downloadTranslationsForAll = project.tasks.register("downloadTranslationsForAll")
        lokaliseExtensions.downloadStringsConfigs.all { downloadConfig ->
            val customDownloadTask = project.tasks.registerDownloadTranslationTask(
                config = downloadConfig,
                downloadAsync = lokaliseExtensions.downloadStringsAsynchronously,
                lokaliseApiFactory = apiFactory,
            )
            downloadTranslationsForAll.configure { allTask -> allTask.dependsOn(customDownloadTask) }

            val checkTranslationTask = project.tasks.registerCheckEverythingTranslatedTask(
                lokaliseApiFactory = apiFactory,
                config = downloadConfig,
            )
            customDownloadTask.configure { downloadTask -> downloadTask.dependsOn(checkTranslationTask) }
        }
    }
}

