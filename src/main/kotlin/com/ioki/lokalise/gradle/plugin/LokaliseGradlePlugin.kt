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
    override fun apply(target: Project) {
        val lokaliseExtensions = target.extensions.createLokaliseExtension()

        val lokaliseApi: Provider<LokaliseApi> = lokaliseExtensions.apiToken.zip(
            lokaliseExtensions.projectId
        ) { token, id ->
            DefaultLokaliseApi(Lokalise(token), id)
        }

        target.tasks.registerUploadTranslationTask(
            lokaliseApi = lokaliseApi,
            lokaliseExtensions = lokaliseExtensions,
        )

        val downloadTranslationsForAll = target.tasks.register("downloadTranslationsForAll")
        lokaliseExtensions.downloadStringsConfigs.all {
            val customDownloadTask = target.tasks.registerDownloadTranslationTask(
                config = it,
                lokaliseExtensions = lokaliseExtensions,
            )
            downloadTranslationsForAll.configure { allTask -> allTask.dependsOn(customDownloadTask) }
        }
    }
}

