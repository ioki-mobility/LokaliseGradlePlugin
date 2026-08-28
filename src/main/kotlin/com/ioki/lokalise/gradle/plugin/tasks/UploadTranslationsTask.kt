package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.api.models.UploadFileRequest
import com.ioki.lokalise.api.models.UploadFileResponse
import com.ioki.lokalise.gradle.plugin.FileInfo
import com.ioki.lokalise.gradle.plugin.LokaliseApiFactory
import com.ioki.lokalise.gradle.plugin.LokaliseExtension
import com.ioki.lokalise.gradle.plugin.LokaliseUploadApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

abstract class UploadTranslationsTask : DefaultTask() {

    @get:Input
    abstract val lokaliseApiFactory: Property<() -> LokaliseUploadApi>

    @get:Input
    @get:Optional
    abstract val pollUploadProcess: Property<Boolean>

    @get:Input
    abstract val translationFilesToUpload: Property<ConfigurableFileTree>

    @get:Input
    abstract val requestBody: Property<UploadFileRequest>

    @TaskAction
    fun f() {
        runBlocking {
            val lokaliseApi = lokaliseApiFactory.get().invoke()
            translationFilesToUpload.get()
                .toFileInfo()
                .also {
                    logger.log(
                        LogLevel.INFO,
                        "Execute uploading file with the following request body:\n" +
                            "${requestBody.get()}\n" +
                            "and the following file info:\n" +
                            "$it"
                    )
                }
                .uploadEach(lokaliseApi, requestBody.get())
                .run { if (pollUploadProcess.get()) checkProcess(lokaliseApi) }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ConfigurableFileTree.toFileInfo(): List<FileInfo> = map {
        val fileName = it.path.replace(dir.absolutePath, ".")
        val base64FileContent = Base64.encode(it.readBytes())
        FileInfo(fileName, base64FileContent)
    }

    private suspend fun List<FileInfo>.uploadEach(
        lokaliseApi: LokaliseUploadApi,
        requestBody: UploadFileRequest,
    ): List<UploadFileResponse> = withContext(Dispatchers.IO) {
        lokaliseApi.uploadFiles(this@uploadEach, requestBody)
    }

    private suspend fun List<UploadFileResponse>.checkProcess(lokaliseApi: LokaliseUploadApi) = withContext(Dispatchers.IO) {
        lokaliseApi.checkProcess(this@checkProcess)
    }
}

internal fun TaskContainer.registerUploadTranslationTask(
    lokaliseApiFactory: LokaliseApiFactory,
    lokaliseExtensions: LokaliseExtension,
): TaskProvider<UploadTranslationsTask> = register("uploadTranslations", UploadTranslationsTask::class.java) {
    it.lokaliseApiFactory.set(lokaliseApiFactory::createUploadApi)
    it.translationFilesToUpload.set(lokaliseExtensions.uploadStringsConfig.translationsFilesToUpload)
    it.pollUploadProcess.set(lokaliseExtensions.pollUploadProcess)
    it.requestBody.set(lokaliseExtensions.uploadStringsConfig.requestBody)
    it.group = "Lokalise"
    it.description = "Upload translations to Lokalise"
}
