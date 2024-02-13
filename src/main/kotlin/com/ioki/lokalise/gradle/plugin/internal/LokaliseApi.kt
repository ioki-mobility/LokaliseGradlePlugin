package com.ioki.lokalise.gradle.plugin.internal

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.Result
import com.ioki.lokalise.api.models.FileUpload
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File

internal interface LokaliseApi {
    suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        langIso: String,
        params: Map<String, Any>
    ): List<FileUpload>
    suspend fun checkProcess(fileUploads: List<FileUpload>)
}

internal abstract class LokaliseApiBuildService : BuildService<LokaliseApiParams>, LokaliseApi {
    private val lokaliseApi = DefaultLokaliseApi(Lokalise(parameters.apiToken.get()), parameters.projectId.get())

    override suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        langIso: String,
        params: Map<String, Any>
    ) = lokaliseApi.uploadFiles(
        fileInfos,
        langIso,
        params,
    )

    override suspend fun checkProcess(fileUploads: List<FileUpload>) = lokaliseApi.checkProcess(fileUploads)
}

internal class DefaultLokaliseApi(
    private val lokalise: Lokalise,
    private val projectId: String,
) : LokaliseApi {

    private val finishedProcessStatus = listOf("cancelled", "finished", "failed")

    override suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        langIso: String,
        params: Map<String, Any>,
    ): List<FileUpload> = coroutineScope {
        val chunkedToSix = fileInfos.chunkedToSix()
        chunkedToSix.flatMapIndexed { index, chunkedFileInfos ->
            val fileUploads = chunkedFileInfos.map { fileInfo ->
                async {
                    val uploadResult = lokalise.uploadFile(
                        projectId = projectId,
                        data = fileInfo.base64FileContent,
                        filename = fileInfo.fileName,
                        langIso = langIso,
                        bodyParams = params
                    )

                    when (uploadResult) {
                        is Result.Failure -> throw GradleException(uploadResult.error.message)
                        is Result.Success -> uploadResult.data
                    }
                }
            }
            if (index != chunkedToSix.lastIndex) delay(1000)
            fileUploads.awaitAll()
        }
    }

    override suspend fun checkProcess(fileUploads: List<FileUpload>) = coroutineScope {
        val chunkedToSix = fileUploads.chunkedToSix()
        chunkedToSix.forEachIndexed { index, chunkedFileUploads ->
            val deferreds = chunkedFileUploads.map {
                async {
                    do {
                        val process = lokalise.retrieveProcess(
                            projectId = projectId,
                            processId = it.process.processId
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
                        delay(500)
                    } while (true)
                }
            }
            if (index != chunkedToSix.lastIndex) delay(1000)
            deferreds.awaitAll()
        }
    }

    /**
     * This is required because Lokalise API only allows 6 files to be uploaded at once.
     * See also [https://lokalise.com/blog/announcing-api-rate-limits/](https://lokalise.com/blog/announcing-api-rate-limits/)
     */
    private fun <T> List<T>.chunkedToSix(): List<List<T>> = chunked(6)
}

interface LokaliseApiParams : BuildServiceParameters {
    val apiToken: Property<String>
    val projectId: Property<String>
}

internal data class FileInfo(
    val fileName: String,
    val base64FileContent: String,
)