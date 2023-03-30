package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadLokaliseCliTask
import com.ioki.lokalise.gradle.plugin.tasks.registerDownloadTranslationTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUnzipLokaliseCliTask
import com.ioki.lokalise.gradle.plugin.tasks.registerUploadTranslationTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val lokaliseExtensions = target.extensions.createLokaliseExtension()

        val downloadLokaliseTask = target.tasks.registerDownloadLokaliseCliTask()
        val unzipLokaliseTask = target.tasks.registerUnzipLokaliseCliTask(
            downloadLokaliseCliTask = downloadLokaliseTask
        )
        target.tasks.registerDownloadTranslationTask(
            lokaliseExtensions = lokaliseExtensions,
            unzipLokaliseTask = unzipLokaliseTask
        )
        target.tasks.registerUploadTranslationTask(
            lokaliseExtensions = lokaliseExtensions,
            unzipLokaliseTask = unzipLokaliseTask
        )
    }
}

