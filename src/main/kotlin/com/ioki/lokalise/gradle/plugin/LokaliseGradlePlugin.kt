package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.internal.LokaliseApiBuildService
import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val lokaliseExtensions = target.extensions.createLokaliseExtension()

        val lokaliseService = target.gradle.sharedServices.registerIfAbsent(
            "LokaliseApi",
            LokaliseApiBuildService::class.java
        ) { buildServiceSpec ->
            buildServiceSpec.parameters { lokaliseApiParams ->
                lokaliseApiParams.apiToken.set(lokaliseExtensions.apiToken)
                lokaliseApiParams.projectId.set(lokaliseExtensions.projectId)
            }
        }

        target.tasks.registerUploadTranslationTask(
            lokaliseService = lokaliseService,
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

