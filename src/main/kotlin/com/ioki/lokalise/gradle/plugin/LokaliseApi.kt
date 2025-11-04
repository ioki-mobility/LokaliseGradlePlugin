package com.ioki.lokalise.gradle.plugin

import com.ioki.lokalise.api.Lokalise
import com.ioki.lokalise.api.models.AsyncExportDetails
import com.ioki.lokalise.api.models.FileDownload
import com.ioki.lokalise.api.models.FileUpload
import com.ioki.lokalise.api.models.Process
import com.ioki.lokalise.api.models.Project
import com.ioki.lokalise.api.models.RetrievedProcess
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

    suspend fun downloadFilesAsync(
        format: String,
        params: Map<String, Any>
    ): FileDownload
}

interface LokaliseProjectApi {
    suspend fun getProject(): Project
}

internal class DefaultLokaliseApi(
    private val lokalise: Lokalise,
    private val projectId: String,
) : LokaliseUploadApi, LokaliseDownloadApi, LokaliseProjectApi {

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
                        is Failure -> throw GradleException("Can't upload files\n${uploadResult.error.message}")
                        is Success -> uploadResult.data
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
            val deferreds = chunkedFileUploads.map { async { awaitProcess(it.projectId) } }
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
            is Failure -> throw GradleException("Can't download files\n${result.error.message}")
            is Success -> result.data
        }
    }

    override suspend fun downloadFilesAsync(
        format: String,
        params: Map<String, Any>
    ): FileDownload {
        val result = lokalise.downloadFilesAsync(
            projectId = projectId,
            format = format,
            bodyParams = params,
        )
        return when (result) {
            is Failure -> throw GradleException("Can't download files\n${result.error.message}")
            is Success -> {
                val checkProcess = awaitProcess(result.data.processId) ?: throw GradleException("Can't download files")
                val asyncExportProcess = checkProcess.process as? Process.AsyncExport
                val asyncExportDetailsFinished = asyncExportProcess?.details as? AsyncExportDetails.Finished
                val downloadUrl = asyncExportDetailsFinished?.downloadUrl
                    ?: throw GradleException("Can't download files, no download URL found")
                FileDownload(
                    projectId = projectId,
                    bundleUrl = downloadUrl,
                )
            }
        }
    }

    override suspend fun getProject(): Project {
        return when (val result = lokalise.allProjects()) {
            is Failure -> throw GradleException("Can't get all project\n${result.error.message}")
            is Success -> result.data.projects.find { it.projectId == projectId }
                ?: throw GradleException("Can't find project with id $projectId")
        }
    }

    /**
     * This is required because Lokalise API only allows 6 files to be uploaded at once.
     * See also [https://lokalise.com/blog/announcing-api-rate-limits/](https://lokalise.com/blog/announcing-api-rate-limits/)
     */
    private fun <T> List<T>.chunkedToSix(): List<List<T>> = chunked(6)

    private suspend fun awaitProcess(processId: String): RetrievedProcess? {
        var latestProcess = null as RetrievedProcess?
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