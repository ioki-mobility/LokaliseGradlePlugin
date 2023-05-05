package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadLokaliseCliTask
import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val lokaliseExtensions = target.extensions.createLokaliseExtension()

        val downloadLokaliseCliTask = target.tasks.registerDownloadLokaliseCliTask()
        target.tasks.registerUploadTranslationTask(
            lokaliseExtensions = lokaliseExtensions,
            lokaliseCliFile = downloadLokaliseCliTask.map { it.lokaliseCliFile }
        )
        lokaliseExtensions.downloadStringsConfigs.all {
            target.tasks.registerDownloadTranslationTask(
                config = it,
                lokaliseExtensions = lokaliseExtensions,
                lokaliseCliFile = downloadLokaliseCliTask.map { it.lokaliseCliFile }
            )
        }
    }
}

