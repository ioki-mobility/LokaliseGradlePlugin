package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URL
import java.nio.file.Files

internal abstract class DownloadLokaliseCliTask : DefaultTask() {
    @Input
    lateinit var lokaliseCliUrl: String

    @OutputFile
    val lokaliseCliZipFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun f() {
        URL(lokaliseCliUrl).openStream().use {
            Files.copy(it, lokaliseCliZipFile.asFile.get().toPath())
        }
    }
}