package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URL
import java.nio.file.Files

internal abstract class DownloadLokaliseCliTask : DefaultTask() {
    @get:Input
    abstract val lokaliseCliUrl: Property<String>

    @get:OutputFile
    abstract val lokaliseCliZipFile: RegularFileProperty

    @TaskAction
    fun f() {
        URL(lokaliseCliUrl.get()).openStream().use {
            Files.copy(it, lokaliseCliZipFile.get().asFile.toPath())
        }
    }
}