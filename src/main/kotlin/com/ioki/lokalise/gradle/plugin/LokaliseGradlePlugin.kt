package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.gradle.plugin.internal.DefaultLokaliseApi
import com.ioki.lokalise.gradle.plugin.internal.LokaliseApi
import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val lokaliseExtensions = project.extensions.createLokaliseExtension()

        val lokaliseApi: Provider<LokaliseApi> = project.providers.zip(
            lokaliseExtensions.apiToken,
            lokaliseExtensions.projectId
        ) { token, id ->
            DefaultLokaliseApi(Lokalise(token), id)
        }

        project.tasks.registerUploadTranslationTask(
            lokaliseApi = lokaliseApi,
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

