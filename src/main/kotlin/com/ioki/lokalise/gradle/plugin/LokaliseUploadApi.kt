package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.Result
import com.ioki.lokalise.api.models.FileDownload
import com.ioki.lokalise.api.models.FileUpload
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

class LokaliseUploadApiFactory(
    private val apiTokenProvider: Provider<String>,
    private val projectIdProvider: Provider<String>,
) {
    fun create(): LokaliseUploadApi = DefaultLokaliseApi(Lokalise(apiTokenProvider.get()), projectIdProvider.get())
}

class LokaliseDownloadApiFactory(
    private val apiTokenProvider: Provider<String>,
    private val projectIdProvider: Provider<String>,
) {
    fun create(): LokaliseDownloadApi = DefaultLokaliseApi(Lokalise(apiTokenProvider.get()), projectIdProvider.get())
}

interface LokaliseUploadApi {
    suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        langIso: String,
        params: Map<String, Any>
    ): List<FileUpload>
    suspend fun checkProcess(fileUploads: List<FileUpload>)
}

interface LokaliseDownloadApi {
    suspend fun downloadFiles(
        format: String,
        params: Map<String, Any>
    ): FileDownload
}


internal class DefaultLokaliseApi(
    private val lokalise: Lokalise,
    private val projectId: String,
) : LokaliseUploadApi, LokaliseDownloadApi {

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

    override suspend fun downloadFiles(format: String, params: Map<String, Any>): FileDownload {
        val result = lokalise.downloadFiles(
            projectId = projectId,
            format = format,
            bodyParams = params,
        )
        return when (result) {
            is Result.Failure -> throw GradleException("Can't download files\n${result.error.message}")
            is Result.Success -> result.data
        }
    }
    /**
     * This is required because Lokalise API only allows 6 files to be uploaded at once.
     * See also [https://lokalise.com/blog/announcing-api-rate-limits/](https://lokalise.com/blog/announcing-api-rate-limits/)
     */
    private fun <T> List<T>.chunkedToSix(): List<List<T>> = chunked(6)
}

data class FileInfo(
    val fileName: String,
    val base64FileContent: String,
)