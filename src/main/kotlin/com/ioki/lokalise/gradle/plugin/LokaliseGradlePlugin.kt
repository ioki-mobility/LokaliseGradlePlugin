package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.gradle.plugin.tasks.DownloadLokaliseCliTask
import com.ioki.lokalise.gradle.plugin.tasks.DownloadTranslationsTask
import com.ioki.lokalise.gradle.plugin.tasks.UnzipLokaliseCliTask
import com.ioki.lokalise.gradle.plugin.tasks.UploadTranslationsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*

class LokaliseGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {

        val lokaliseExtensions = target.extensions.createLokaliseExtension()

        val downloadLokaliseTask = target.tasks.register("downloadLokaliseCli", DownloadLokaliseCliTask::class.java) {
            it.lokaliseCliUrl = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
                "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_darwin_arm64.tar.gz"
            } else {
                "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_linux_x86_64.tar.gz"
            }
            it.lokaliseCliZipFile.set(File(target.buildDir.toString() + "/lokalise", "lokalise_cli.tar.gz"))
        }
        val unzipLokaliseTask = target.tasks.register("unzipLokaliseCli", UnzipLokaliseCliTask::class.java) { copyTask ->
            copyTask.from(target.tarTree(downloadLokaliseTask.map { task -> task.lokaliseCliZipFile.get() }))
            copyTask.rename("lokalise2", "lokalise")
            copyTask.into(target.buildDir.toString() + "/lokalise/cli")
        }
        target.tasks.register("downloadTranslations", DownloadTranslationsTask::class.java) {
            it.apiToken = lokaliseExtensions.apiToken
            it.projectId = lokaliseExtensions.projectId
            it.lokaliseOutputDir = unzipLokaliseTask.map { task -> task.destinationDir }
        }
        target.tasks.register("uploadTranslations", UploadTranslationsTask::class.java) {
            it.apiToken = lokaliseExtensions.apiToken
            it.projectId = lokaliseExtensions.projectId
            it.lokaliseCliOutputDir = unzipLokaliseTask.map { task -> task.destinationDir }
            it.translationFilesToUpload = lokaliseExtensions.translationsFilesToUpload
        }

    }
}