package com.ioki.lokalise.gradle.plugin.tasks

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.Result
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

    @OptIn(ExperimentalEncodingApi::class)
    @TaskAction
    fun f() {
        if (!projectId.isPresent || !apiToken.isPresent)
            throw GradleException("Please set 'lokalise.projectId' and 'lokalise.apiToken'")

        val fileTree = translationFilesToUpload.get()
        val fileInfo = fileTree.map {
            val fileName = it.path.replace(fileTree.dir.absolutePath, ".")
            val base64FileContent = Base64.encode(it.readBytes())
            fileName to base64FileContent
        }

        logger.log(
            LogLevel.INFO,
            "Execute uploading file with the following params:\n" +
                "${params.get()}\n" +
                "and the following file info:\n" +
                "$fileInfo"
        )

        val langIso = params.get()["lang_iso"]
        val newParams = params.get().toMutableMap().apply { remove("lang_iso") }

        val lokalise = Lokalise(apiToken.get())
        val fileUploads = fileInfo.map { (fileName, base64FileContent) ->
            runBlocking {
                val fileUploadResult = lokalise.uploadFile(
                    projectId = projectId.get(),
                    data = base64FileContent,
                    filename = fileName,
                    langIso = langIso.toString(),
                    bodyParams = newParams
                )
                when (fileUploadResult) {
                    is Result.Failure -> throw GradleException(fileUploadResult.error.message)
                    is Result.Success -> fileUploadResult.data
                }
            }
        }

        runBlocking {
            fileUploads
                .map { fileUpload ->
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
}

private val finishedProcessStatus = listOf("cancelled", "finished", "failed")

internal fun TaskContainer.registerUploadTranslationTask(
    lokaliseExtensions: LokaliseExtension,
): TaskProvider<UploadTranslationsTask> = register("uploadTranslations", UploadTranslationsTask::class.java) {
    it.apiToken.set(lokaliseExtensions.apiToken)
    it.projectId.set(lokaliseExtensions.projectId)
    it.translationFilesToUpload.set(lokaliseExtensions.uploadStringsConfig.translationsFilesToUpload)
    it.params.set(lokaliseExtensions.uploadStringsConfig.params)
}