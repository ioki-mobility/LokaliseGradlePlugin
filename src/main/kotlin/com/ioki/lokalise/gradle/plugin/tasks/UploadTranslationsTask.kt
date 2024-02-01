package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.Result
import com.ioki.lokalise.api.models.FileUpload
import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal abstract class UploadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val projectId: Property<String>

    @get:Input
    abstract val apiToken: Property<String>

    @get:Input
    abstract val translationFilesToUpload: Property<ConfigurableFileTree>

    @get:Input
    abstract val params: MapProperty<String, Any>

    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val lokalise = Lokalise(apiToken.get())
        translationFilesToUpload.get()
            .toFileInfo()
            .also {
                logger.log(
                    LogLevel.INFO,
                    "Execute uploading file with the following params:\n" +
                        "${params.get()}\n" +
                        "and the following file info:\n" +
                        "$it"
                )
            }
            .uploadEach(lokalise, params.get("lang_iso").toString(), params.remove("lang_iso"))
            .checkProcess(lokalise)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ConfigurableFileTree.toFileInfo(): List<FileInfo> = map {
        val fileName = it.path.replace(dir.absolutePath, ".")
        val base64FileContent = Base64.encode(it.readBytes())
        FileInfo(fileName, base64FileContent)
    }

    private fun List<FileInfo>.uploadEach(
        lokalise: Lokalise,
        langIso: String,
        params: Map<String, Any>,
    ): List<FileUpload> = map { fileInfo ->
        val uploadResult = runBlocking {
            lokalise.uploadFile(
                projectId = projectId.get(),
                data = fileInfo.base64FileContent,
                filename = fileInfo.fileName,
                langIso = langIso,
                bodyParams = params
            )
        }
        when (uploadResult) {
            is Result.Failure -> throw GradleException(uploadResult.error.message)
            is Result.Success -> uploadResult.data
        }
    }

    private fun List<FileUpload>.checkProcess(lokalise: Lokalise) = runBlocking {
        forEach { fileUpload ->
            do {
                val process = lokalise.retrieveProcess(
                    projectId = projectId.get(),
                    processId = fileUpload.process.processId
                )
                when (process) {
                    is Result.Failure -> {
                        if (process.error.code == 404) {
                            // 404 indicates it is done... I guess :)
                            break
                        }
                    }

                    is Result.Success -> {
                        val processStatus = process.data.process.status
                        if (finishedProcessStatus.contains(processStatus)) {
                            break
                        }
                    }
                }

                Thread.sleep(1000)
            } while (true)
        }
    }
}

internal fun TaskContainer.registerUploadTranslationTask(
    lokaliseExtensions: LokaliseExtension,
): TaskProvider<UploadTranslationsTask> = register("uploadTranslations", UploadTranslationsTask::class.java) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.translationFilesToUpload.set(lokaliseExtensions.uploadStringsConfig.translationsFilesToUpload)
    it.params.set(lokaliseExtensions.uploadStringsConfig.params)
}

private val finishedProcessStatus = listOf("cancelled", "finished", "failed")

private data class FileInfo(
    val fileName: String,
    val base64FileContent: String,
)

private fun MapProperty<String, Any>.get(key: String): Any =
    get().getOrElse(key) { throw GradleException("Value for key(=$key) not found") }

private fun MapProperty<String, Any>.remove(key: String): Map<String, Any> =
    get().toMutableMap().apply { remove(key) }
