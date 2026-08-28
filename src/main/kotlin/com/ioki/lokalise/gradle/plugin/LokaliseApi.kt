package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.models.AsyncExportDetails
import com.ioki.lokalise.api.models.DownloadFilesRequest
import com.ioki.lokalise.api.models.DownloadFilesResponse
import com.ioki.lokalise.api.models.Process
import com.ioki.lokalise.api.models.RetrieveProcessResponse
import com.ioki.lokalise.api.models.RetrieveProjectResponse
import com.ioki.lokalise.api.models.UploadFileRequest
import com.ioki.lokalise.api.models.UploadFileResponse
import com.ioki.result.Result.Failure
import com.ioki.result.Result.Success
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider

class LokaliseApiFactory(
    private val apiTokenProvider: Provider<String>,
    private val projectIdProvider: Provider<String>,
) {
    fun createUploadApi(): LokaliseUploadApi = createLokaliseApi()
    fun createDownloadApi(): LokaliseDownloadApi = createLokaliseApi()
    fun createProjectApi(): LokaliseProjectApi = createLokaliseApi()
    private fun createLokaliseApi(): DefaultLokaliseApi =
        DefaultLokaliseApi(Lokalise(apiTokenProvider.get(), false), projectIdProvider.get())
}

interface LokaliseUploadApi {
    suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        requestBody: UploadFileRequest,
    ): List<UploadFileResponse>

    suspend fun checkProcess(fileUploads: List<UploadFileResponse>)
}

interface LokaliseDownloadApi {
    suspend fun downloadFiles(requestBody: DownloadFilesRequest): DownloadFilesResponse

    suspend fun downloadFilesAsync(requestBody: DownloadFilesRequest): DownloadFilesResponse
}

interface LokaliseProjectApi {
    suspend fun getProject(): RetrieveProjectResponse
}

internal class DefaultLokaliseApi(
    private val lokalise: Lokalise,
    private val projectId: String,
) : LokaliseUploadApi, LokaliseDownloadApi, LokaliseProjectApi {

    private val finishedProcessStatus = listOf("cancelled", "finished", "failed")

    override suspend fun uploadFiles(
        fileInfos: List<FileInfo>,
        requestBody: UploadFileRequest,
    ): List<UploadFileResponse> = coroutineScope {
        val chunkedToSix = fileInfos.chunkedToSix()
        chunkedToSix.flatMapIndexed { index, chunkedFileInfos ->
            val fileUploads = chunkedFileInfos.map { fileInfo ->
                async {
                    val uploadResult = lokalise.uploadFile(
                        projectId = projectId,
                        requestBody = requestBody.copy(
                            data = fileInfo.base64FileContent,
                            filename = fileInfo.fileName,
                        ),
                    )

                    when (uploadResult) {
                        is Failure -> throw GradleException("Can't upload files\n${uploadResult.error.message}")
                        is Success -> uploadResult.data
                    }
                }
            }
            if (index != chunkedToSix.lastIndex) delay(1000)
            fileUploads.awaitAll()
        }
    }

    override suspend fun checkProcess(fileUploads: List<UploadFileResponse>) = coroutineScope {
        val chunkedToSix = fileUploads.chunkedToSix()
        chunkedToSix.forEachIndexed { index, chunkedFileUploads ->
            val deferreds = chunkedFileUploads.map { async { awaitProcess(it.process.processId) } }
            if (index != chunkedToSix.lastIndex) delay(1000)
            deferreds.awaitAll()
        }
    }

    override suspend fun downloadFiles(requestBody: DownloadFilesRequest): DownloadFilesResponse {
        val result = lokalise.downloadFiles(
            projectId = projectId,
            requestBody = requestBody,
        )
        return when (result) {
            is Failure -> throw GradleException("Can't download files\n${result.error.message}")
            is Success -> result.data
        }
    }

    override suspend fun downloadFilesAsync(requestBody: DownloadFilesRequest): DownloadFilesResponse {
        val result = lokalise.downloadFilesAsync(
            projectId = projectId,
            requestBody = requestBody,
        )
        return when (result) {
            is Failure -> throw GradleException("Can't download files\n${result.error.message}")
            is Success -> {
                val checkProcess = awaitProcess(result.data.processId) ?: throw GradleException("Can't download files")
                val asyncExportProcess = checkProcess.process as? Process.AsyncExport
                val asyncExportDetailsFinished = asyncExportProcess?.details as? AsyncExportDetails.Finished
                val downloadUrl = asyncExportDetailsFinished?.downloadUrl
                    ?: throw GradleException("Can't download files, no download URL found")
                DownloadFilesResponse(
                    projectId = projectId,
                    bundleUrl = downloadUrl,
                )
            }
        }
    }

    override suspend fun getProject(): RetrieveProjectResponse {
        return when (val result = lokalise.retrieveProject(projectId)) {
            is Failure -> throw GradleException("Can't get project\n${result.error.message}")
            is Success -> result.data
        }
    }

    /**
     * This is required because Lokalise API only allows 6 files to be uploaded at once.
     * See also [https://lokalise.com/blog/announcing-api-rate-limits/](https://lokalise.com/blog/announcing-api-rate-limits/)
     */
    private fun <T> List<T>.chunkedToSix(): List<List<T>> = chunked(6)

    private suspend fun awaitProcess(processId: String): RetrieveProcessResponse? {
        var latestProcess = null as RetrieveProcessResponse?
        do {
            val processResult = lokalise.retrieveProcess(
                projectId = projectId,
                processId = processId
            )

            when (processResult) {
                is Failure -> {
                    if (processResult.error.code == 404) {
                        // 404 indicates it is done... I guess :)
                        break
                    }
                }

                is Success -> {
                    latestProcess = processResult.data
                    val processStatus = latestProcess.process.status
                    if (finishedProcessStatus.contains(processStatus)) {
                        break
                    }
                }
            }
            delay(500)
        } while (true)
        return latestProcess
    }
}

data class FileInfo(
    val fileName: String,
    val base64FileContent: String,
)
