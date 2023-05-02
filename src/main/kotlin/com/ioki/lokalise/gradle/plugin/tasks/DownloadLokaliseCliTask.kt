package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.net.URL
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal abstract class DownloadLokaliseCliTask : DefaultTask() {
    @get:Input
    abstract val lokaliseCliUrl: Property<String>

    @get:OutputFile
    abstract val lokaliseCliZipFile: RegularFileProperty

    @TaskAction
    fun f() {
        URL(lokaliseCliUrl.get()).openStream().use {
            Files.copy(it, lokaliseCliZipFile.get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal fun TaskContainer.registerDownloadLokaliseCliTask(): TaskProvider<DownloadLokaliseCliTask> =
    register("downloadLokaliseCli", DownloadLokaliseCliTask::class.java) {
        it.lokaliseCliUrl.set(findCliUrl())
        it.lokaliseCliZipFile.set(File(it.project.buildDir.toString() + "/lokalise", "lokalise_cli.tar.gz"))
    }

private fun findCliUrl(): String = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
    "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_darwin_arm64.tar.gz"
} else {
    "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_linux_x86_64.tar.gz"
}