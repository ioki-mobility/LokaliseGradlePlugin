package com.ioki.lokalise.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import javax.inject.Inject
import kotlin.io.path.moveTo

internal abstract class DownloadLokaliseCliTask : DefaultTask() {

    @get:Input
    abstract val lokaliseCliUrl: Property<String>

    @get:OutputFile
    abstract val lokaliseCliTarFile: RegularFileProperty

    @get:OutputFile
    abstract val lokaliseCliFile: RegularFileProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @TaskAction
    fun f() {
        val cliTarFilePath = lokaliseCliTarFile.get().asFile.toPath()
        downloadCli(cliTarFilePath)
        unTarAndMoveCli(cliTarFilePath)
    }

    private fun downloadCli(cliTarFilePath: Path) {
        URL(lokaliseCliUrl.get()).openStream().use {
            Files.copy(it, cliTarFilePath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun unTarAndMoveCli(cliTarFilePath: Path) {
        archiveOperations.tarTree(cliTarFilePath).singleFile.toPath()
            .moveTo(lokaliseCliFile.get().asFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

internal fun TaskContainer.registerDownloadLokaliseCliTask(): TaskProvider<DownloadLokaliseCliTask> =
    register("downloadLokaliseCli", DownloadLokaliseCliTask::class.java) {
        it.lokaliseCliUrl.set(findCliUrl())
        val lokaliseBuildDir = it.project.buildDir.resolve("lokalise")
        it.lokaliseCliTarFile.set(lokaliseBuildDir.resolve("lokalise_cli.tar.gz"))
        it.lokaliseCliFile.set(lokaliseBuildDir.resolve("cli"))
    }

private fun findCliUrl(): String = if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
    "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_darwin_arm64.tar.gz"
} else {
    "https://github.com/lokalise/lokalise-cli-2-go/releases/download/v2.6.8/lokalise2_linux_x86_64.tar.gz"
}